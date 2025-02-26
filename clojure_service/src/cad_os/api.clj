(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [cad-os.models.washer :as washer-model]
            [clojure.java.io :as io]
            [cheshire.core :as cheshire])
  (:gen-class))

(defn handle-model-request
  "Generic handler for model creation requests"
  [create-fn request]
  (println "Handling model request")
  (try
    (let [params (:body request)
          result (create-fn params)]
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

(defroutes app-routes
  (GET "/" [] "CAD-OS API is running")

  (POST "/generate/washer" req
    (handle-model-request washer-model/create req))

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

(route/not-found "Not Found")

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main [& args]
  (println "Starting CAD-OS API server on port 3000...")
  (run-jetty app {:port 3000 :join? false})
  (println "Server started!"))