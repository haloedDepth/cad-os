(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.data.json :as json]
            [cad-os.core :as core]
            [cad-os.obj :as obj]
            [clojure.java.io :as io]
            [cheshire.core :as cheshire])
  (:gen-class))

(defn generate-washer [params]
  (println "Received params:" params)
  
  ;; Helper function to get a value regardless of whether the key is a keyword, symbol, or string
  (defn get-param [params key]
    (or (get params (keyword key))     ;; Try as keyword
        (get params (str key))         ;; Try as string
        (get params (symbol key))))    ;; Try as symbol
  
  (if (or (nil? params)
          (not (map? params)))
    {:status "error"
     :message "Invalid parameters. Required: outer-diameter, inner-diameter, thickness"}
    
    (let [outer-diameter (str (get-param params "outer-diameter"))
          inner-diameter (str (get-param params "inner-diameter"))
          thickness (str (get-param params "thickness"))
          _ (println "Parsed parameters:" outer-diameter inner-diameter thickness)
          
          ;; Check if any required parameters are missing
          missing-params (filter #(or (nil? %) 
                                     (= "nil" %) 
                                     (empty? %))
                               [outer-diameter inner-diameter thickness])]
      
      (if (seq missing-params)
        {:status "error"
         :message "Missing required parameters: outer-diameter, inner-diameter, thickness"}
        
        (let [file-name (str "washer_" outer-diameter "_" inner-diameter "_" thickness)
              
              ;; Parse values safely
              outer-d (try (Double/parseDouble outer-diameter)
                          (catch Exception e 
                            (println "Error parsing outer-diameter:" (.getMessage e)) 
                            10.0))
              inner-d (try (Double/parseDouble inner-diameter)
                          (catch Exception e 
                            (println "Error parsing inner-diameter:" (.getMessage e)) 
                            6.0))
              thick (try (Double/parseDouble thickness)
                        (catch Exception e 
                          (println "Error parsing thickness:" (.getMessage e)) 
                          2.0))
              
              _ (println "Creating .g file with params:" outer-d inner-d thick)
              g-result (core/create-g file-name core/washer outer-d inner-d thick)
              
              ;; Use improved obj conversion with the correct object name "washer"
              _ (println "Converting to OBJ format")
              obj-result (obj/convert-g-to-obj file-name ["washer"] 
                                              {:mesh true, :verbose true, :abs-tess-tol 0.01})]
          
          {:file-name file-name
           :g-result g-result
           :obj-result obj-result
           :obj-path (get obj-result :file (str file-name ".obj"))})))))

(defroutes app-routes
  (GET "/" [] "CAD-OS API is running")
  
  (POST "/generate/washer" req
    (println "Handling /generate/washer request")
    (println "Request body: " (:body req))
    (try
      (let [result (generate-washer (:body req))]
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
      (wrap-json-body {:keywords? true})  ;; Force keywords with true
      wrap-json-response))

(defn -main [& args]
  (println "Starting CAD-OS API server on port 3000...")
  (run-jetty app {:port 3000 :join? false})
  (println "Server started!"))