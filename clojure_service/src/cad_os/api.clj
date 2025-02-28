(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [cad-os.models.registry :as registry]
            [cad-os.formats :as formats]
            [cad-os.render :as render]
            [cad-os.utils.logger :as logger]
            [cad-os.utils.errors :as errors])
  (:gen-class))

;; Initialize logger
(def log (logger/get-logger))

(defn handle-model-request
  "Generic handler for model creation requests with improved error handling"
  [create-fn request]
  ((:info log) "Handling model request" {:body (:body request)})
  (try
    (let [params (:body request)
          _ ((:debug log) "Processing params" {:params params})

          ;; Extract format request if present (in future API versions)
          requested-formats (get params :formats nil)
          _ (when requested-formats
              ((:info log) "Client requested specific formats" {:formats requested-formats}))

          ;; For web requests, create both .g and .obj by default for backward compatibility
          formats #{:g :obj}

          ;; If specific formats were requested, parse them
          formats (if requested-formats
                    (into #{} (map keyword requested-formats))
                    formats)

          ;; Create model with specified formats
          result (create-fn params :formats formats)]

      ((:info log) "Model creation result" {:status (:status result)})
      (if (= (:status result) "error")
        ;; Return error response
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body result}
        ;; Return success response
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body result}))
    (catch clojure.lang.ExceptionInfo e
      ;; Handle ex-info exceptions
      (let [data (ex-data e)]
        ((:error log) "Error processing request" {:message (.getMessage e) :data data} e)
        (errors/handle-exception e)))
    (catch Exception e
      ;; Handle generic exceptions
      ((:error log) "Unexpected error processing request" {} e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:status "error"
              :message (str "Internal server error: " (.getMessage e))}})))

(defn get-model-file
  "Get a model file in the specified format with improved error handling"
  [filename format]
  ((:info log) "Handling get-model-file request" {:filename filename :format format})
  (let [base-name (if (.endsWith filename ".obj")
                    (clojure.string/replace filename #"\.obj$" "")
                    filename)
        format-keyword (keyword format)
        result (formats/ensure-format base-name format-keyword)]

    ((:debug log) "Format conversion result" {:result result})

    (if (= (:status result) "success")
      (let [file (io/file (:file result))]
        ((:info log) "Serving file" {:file (.getAbsolutePath file)})
        (if (.exists file)
          {:status 200
           :headers {"Content-Type" "application/octet-stream"
                     "Content-Disposition" (str "attachment; filename=\""
                                                (.getName file) "\"")}
           :body file}
          (errors/not-found-error (str "File not found: " (.getAbsolutePath file))
                                  {:filename filename
                                   :format format})))
      (errors/server-error (:message result)
                           {:filename filename
                            :format format}))))

(defroutes app-routes
  (GET "/" []
    (do
      ((:info log) "Root endpoint accessed")
      "CAD-OS API is running"))

  ;; Get list of available model types
  (GET "/models/types" []
    ((:info log) "Getting model types")
    (let [types (registry/get-model-types)]
      ((:info log) "Available types" {:types types})
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:model_types types}}))

  ;; Get all schemas
  (GET "/models/schemas" []
    ((:info log) "Getting all model schemas")
    (let [schemas (registry/get-all-schemas)]
      ((:info log) "Returning schemas" {:count (count schemas)})
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:schemas schemas}}))

  ;; Get schema for a specific model type
  (GET "/models/schema/:type" [type]
    ((:info log) "Getting schema for type" {:type type})
    (if-let [schema (registry/get-model-schema type)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body schema}
      (errors/not-found-error (str "Unknown model type: " type)
                              {:type type})))

  ;; Generate a model of specified type
  (POST "/generate/:type" [type :as req]
    ((:info log) "Generating model of type" {:type type})
    (handle-model-request (partial registry/create-model type) req))

  ;; Get model in specific format
  (GET "/models/:filename/:format" [filename format]
    ((:info log) "Handling model request" {:filename filename :format format})
    ((:debug log) "Full requested filename" {:filename filename})
    (get-model-file filename format))

  ;; Original model endpoint
  (GET "/models/:filename" [filename]
    ((:info log) "Handling model request with default format" {:filename filename})
    (let [obj-file (io/file (if (.endsWith filename ".obj")
                              filename
                              (str filename ".obj")))]
      ((:debug log) "Looking for file" {:path (.getAbsolutePath obj-file)})
      (if (.exists obj-file)
        {:status 200
         :headers {"Content-Type" "application/octet-stream"
                   "Content-Disposition" (str "attachment; filename=\""
                                              (if (.endsWith filename ".obj")
                                                filename
                                                (str filename ".obj"))
                                              "\"")}
         :body obj-file}
        (errors/not-found-error (str "File not found: " (.getAbsolutePath obj-file))
                                {:filename filename}))))

  ;; Endpoint for rendering with default view (front)
  (GET "/render/:filename" [filename :as request]
    ((:info log) "Handling render request with default view" {:filename filename})
    (let [model-type (get-in request [:params :model_type])
          size (get-in request [:params :size] 800)
          white-bg (get-in request [:params :white_background] true)

          ;; Create a temporary directory for rendering
          output-dir (str "render_output/" filename)
          _ (io/make-parents (str output-dir "/placeholder"))

          ;; Generate the view with default (front) view
          render-options {:size size
                          :white-background white-bg}

          ;; Call the render function with default front view
          result (render/generate-orbit-view
                  filename
                  (if model-type [model-type] [])  ;; Handle nil model-type properly
                  0  ;; Default azimuth for front view
                  30 ;; Default elevation for front view
                  (str output-dir "/" filename "_front.png")
                  render-options)]

      (if (= (:status result) "success")
        {:status 200
         :headers {"Content-Type" "image/png"
                   "Content-Disposition" (str "attachment; filename=\""
                                              filename "_front.png\"")}
         :body (io/file (:file result))}
        (errors/server-error (:message result)
                             {:filename filename
                              :view "front"
                              :model_type model-type}))))

  (GET "/render/:filename/:view" [filename view :as request]
    ((:info log) "Handling render request" {:filename filename :view view})
    (let [model-type (get-in request [:params :model_type])
          size (get-in request [:params :size] 800)
          white-bg (get-in request [:params :white_background] true)

          ;; Determine views based on the view parameter
          view-keyword (keyword view)

          ;; Create a temporary directory for rendering
          output-dir (str "render_output/" filename)
          _ (io/make-parents (str output-dir "/placeholder"))

          ;; Generate the view
          render-options {:size size
                          :white-background white-bg}

          ;; Call the render function
          result (case view-keyword
                   :front (render/generate-orbit-view
                           filename
                           (if model-type [model-type] [])
                           0
                           30
                           (str output-dir "/" filename "_front.png")
                           render-options)
                   :right (render/generate-orbit-view
                           filename
                           (if model-type [model-type] [])
                           90
                           30
                           (str output-dir "/" filename "_right.png")
                           render-options)
                   :back (render/generate-orbit-view
                          filename
                          (if model-type [model-type] [])
                          180
                          30
                          (str output-dir "/" filename "_back.png")
                          render-options)
                   :left (render/generate-orbit-view
                          filename
                          (if model-type [model-type] [])
                          270
                          30
                          (str output-dir "/" filename "_left.png")
                          render-options)
                   :top (render/generate-orbit-view
                         filename
                         (if model-type [model-type] [])
                         0
                         90
                         (str output-dir "/" filename "_top.png")
                         render-options)
                   ;; Default to front view
                   (render/generate-orbit-view
                    filename
                    (if model-type [model-type] [])
                    0
                    30
                    (str output-dir "/" filename "_front.png")
                    render-options))]

      (if (= (:status result) "success")
        {:status 200
         :headers {"Content-Type" "image/png"
                   "Content-Disposition" (str "attachment; filename=\""
                                              filename "_" (name view-keyword) ".png\"")}
         :body (io/file (:file result))}
        (errors/server-error (:message result)
                             {:filename filename
                              :view view
                              :model_type model-type})))))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      errors/wrap-exception-handling))

(defn -main [& args]
  ;; Set log level
  (logger/set-min-log-level! :info)

  ;; Clear the registry before starting to avoid duplicates
  ((:info log) "Clearing model registry")
  (reset! registry/model-registry {})

  ;; These requires will trigger the registration
  ((:info log) "Loading model namespaces")
  (require 'cad-os.models.washer)
  (require 'cad-os.models.cylinder)

  ((:info log) "Starting CAD-OS API server on port 3000")
  ((:info log) "Available model types" {:types (registry/get-model-types)})
  (run-jetty app {:port 3000 :join? false})
  ((:info log) "Server started!"))