(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [cad-os.models.registry :as registry]
            [cad-os.formats :as formats]
            [cad-os.render :as render])
  (:gen-class))

(defn handle-model-request
  "Generic handler for model creation requests"
  [create-fn request]
  (println "Handling model request with body:" (:body request))
  (try
    (let [params (:body request)
          _ (println "Processing params:" params)

          ;; Extract format request if present (in future API versions)
          requested-formats (get params :formats nil)
          _ (when requested-formats
              (println "Client requested specific formats:" requested-formats))

          ;; For web requests, create both .g and .obj by default for backward compatibility
          formats #{:g :obj}

          ;; If specific formats were requested, parse them
          formats (if requested-formats
                    (into #{} (map keyword requested-formats))
                    formats)

          ;; Create model with specified formats
          result (create-fn params :formats formats)]

      (println "Model creation result:" result)
      (if (= (:status result) "error")
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body result}
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body result}))
    (catch Exception e
      (println "Error processing request:" (.getMessage e))
      (.printStackTrace e)
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:status "error"
              :message (str "Internal server error: " (.getMessage e))}})))

(defn get-model-file
  "Get a model file in the specified format"
  [filename format]
  (println "Handling get-model-file request for" filename "in format" format)
  (let [base-name (if (.endsWith filename ".obj")
                    (clojure.string/replace filename #"\.obj$" "")
                    filename)
        format-keyword (keyword format)
        result (formats/ensure-format base-name format-keyword)]

    (println "Format conversion result:" result)

    (if (= (:status result) "success")
      (let [file (io/file (:file result))]
        (println "Serving file:" (.getAbsolutePath file))
        (if (.exists file)
          {:status 200
           :headers {"Content-Type" "application/octet-stream"
                     "Content-Disposition" (str "attachment; filename=\""
                                                (.getName file) "\"")}
           :body file}
          {:status 404
           :headers {"Content-Type" "application/json"}
           :body {:error (str "File not found: " (.getAbsolutePath file))}}))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body {:error (:message result)}})))

(defroutes app-routes
  (GET "/" [] "CAD-OS API is running")

  ;; Get list of available model types
  (GET "/models/types" []
    (println "Getting model types")
    (let [types (registry/get-model-types)]
      (println "Available types:" types)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:model_types types}}))

  ;; Get all schemas
  (GET "/models/schemas" []
    (println "Getting all model schemas")
    (let [schemas (registry/get-all-schemas)]
      (println "Returning schemas for" (count schemas) "models")
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:schemas schemas}}))

  ;; Get schema for a specific model type
  (GET "/models/schema/:type" [type]
    (println "Getting schema for type:" type)
    (if-let [schema (registry/get-model-schema type)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body schema}
      {:status 404
       :headers {"Content-Type" "application/json"}
       :body {:status "error"
              :message (str "Unknown model type: " type)}}))

  ;; Generate a model of specified type
  (POST "/generate/:type" [type :as req]
    (println "Generating model of type:" type)
    (handle-model-request (partial registry/create-model type) req))

  ;; Get model in specific format
  (GET "/models/:filename/:format" [filename format]
    (println "Handling /models/" filename "/" format "request")
    (println "Full requested filename:" filename)
    (let [result (get-model-file filename format)]
      (println "Response status:" (:status result))
      result))

  ;; Original model endpoint
  (GET "/models/:filename" [filename]
    (println "Handling /models/" filename "request")
    (let [obj-file (io/file (if (.endsWith filename ".obj")
                              filename
                              (str filename ".obj")))]
      (println "Looking for file:" (.getAbsolutePath obj-file))
      (if (.exists obj-file)
        {:status 200
         :headers {"Content-Type" "application/octet-stream"
                   "Content-Disposition" (str "attachment; filename=\""
                                              (if (.endsWith filename ".obj")
                                                filename
                                                (str filename ".obj"))
                                              "\"")}
         :body obj-file}
        {:status 404
         :headers {"Content-Type" "application/json"}
         :body {:error (str "File not found: " (.getAbsolutePath obj-file))}})))

  ;; NEW ROUTE: Add endpoint for rendering with default view (front)
  (GET "/render/:filename" [filename :as request]
    (println "Handling /render/" filename " request with default view")
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
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body {:error (:message result)}})))

  (GET "/render/:filename/:view" [filename view :as request]
    (println "Handling /render/" filename "/" view "request")
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
                           (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
                           0
                           30
                           (str output-dir "/" filename "_front.png")
                           render-options)
                   :right (render/generate-orbit-view
                           filename
                           (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
                           90
                           30
                           (str output-dir "/" filename "_right.png")
                           render-options)
                   :back (render/generate-orbit-view
                          filename
                          (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
                          180
                          30
                          (str output-dir "/" filename "_back.png")
                          render-options)
                   :left (render/generate-orbit-view
                          filename
                          (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
                          270
                          30
                          (str output-dir "/" filename "_left.png")
                          render-options)
                   :top (render/generate-orbit-view
                         filename
                         (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
                         0
                         90
                         (str output-dir "/" filename "_top.png")
                         render-options)
                   ;; Default to front view
                   (render/generate-orbit-view
                    filename
                    (if model-type [model-type] [])  ;; FIXED: Handle nil model-type
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
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body {:error (:message result)}}))))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main [& args]
  ;; Clear the registry before starting to avoid duplicates
  (println "Clearing model registry...")
  (reset! registry/model-registry {})

  ;; These requires will trigger the registration
  (println "Loading model namespaces...")
  (require 'cad-os.models.washer)
  (require 'cad-os.models.cylinder)

  (println "Starting CAD-OS API server on port 3000...")
  (println "Available model types:" (registry/get-model-types))
  (run-jetty app {:port 3000 :join? false})
  (println "Server started!")) 