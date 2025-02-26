(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [cad-os.models.registry :as registry]
            [cad-os.formats :as formats])
  (:gen-class))

(defn handle-model-request
  "Generic handler for model creation requests"
  [create-fn request]
  (println "Handling model request with body:" (:body request))
  (try
    (let [params (:body request)
          _ (println "Processing params:" params)
          result (create-fn params)]
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

  ;; Get model in specific format (new endpoint)
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
         :body {:error (str "File not found: " (.getAbsolutePath obj-file))}}))))

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