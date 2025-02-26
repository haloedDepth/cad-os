(ns cad-os.models.washer
  (:require [cad-os.models.core :as model-core]
            [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; Schema function to describe the model parameters
(defn schema
  "Return a schema description for washer parameters"
  []
  {:name "Washer"
   :description "A simple washer (ring) with inner and outer diameters"
   :parameters
   [{:name "outer-diameter"
     :type "number"
     :description "Outer diameter of the washer"
     :default 10.0
     :min 0.1}
    {:name "inner-diameter"
     :type "number"
     :description "Inner diameter of the washer"
     :default 6.0
     :min 0.1}
    {:name "thickness"
     :type "number"
     :description "Thickness of the washer"
     :default 2.0
     :min 0.1}]})

;; Parse and validate washer parameters
(defn parse-params
  "Parse and validate washer parameters from a request map"
  [params]
  (println "Washer parse-params called with:" params)
  (let [param-specs [{:name "outer-diameter" :min 0.1}
                     {:name "inner-diameter" :min 0.1}
                     {:name "thickness" :min 0.1}]
        result (model-core/parse-numeric-params params param-specs)]
    (println "Initial parse result:" result)
    (if (:valid result)
      (let [{:keys [outer-diameter inner-diameter]} (:params result)]
        (if (>= inner-diameter outer-diameter)
          {:valid false
           :message "Inner diameter must be less than outer diameter"}
          result))
      result)))

(defn generate-commands
  "Generate commands to create a washer model"
  [outer-diameter inner-diameter thickness]
  (println "Generating washer commands with: outer=" outer-diameter "inner=" inner-diameter "thickness=" thickness)
  [(commands/insert-right-circular-cylinder "outer" 0 0 0 0 0 thickness (/ outer-diameter 2))
   (commands/insert-right-circular-cylinder "inner" 0 0 0 0 0 thickness (/ inner-diameter 2))
   (commands/subtraction "washer" "outer" "inner")])

(defn get-file-name
  "Generate a file name for a washer based on its parameters"
  [params]
  (let [parsed (parse-params params)]
    (if (:valid parsed)
      (let [{:keys [outer-diameter inner-diameter thickness]} (:params parsed)]
        (str "washer_" outer-diameter "_" inner-diameter "_" thickness))
      nil)))

(defn create
  "Create a washer model from request parameters"
  [params]
  (println "Creating washer with params:" params)
  (try
    (let [parsed (parse-params params)]
      (println "Parsed params result:" parsed)
      (if (:valid parsed)
        (let [{:keys [outer-diameter inner-diameter thickness]} (:params parsed)
              file-name (get-file-name params)
              _ (println "Using file name:" file-name)
              commands (generate-commands outer-diameter inner-diameter thickness)]
          (println "Generated commands:" commands)
          (model-core/create-model file-name "washer" commands))
        {:status "error", :message (or (:message parsed) "Invalid parameters")}))
    (catch Exception e
      (println "Exception in washer creation:" (.getMessage e))
      (.printStackTrace e)
      {:status "error", :message (str "Error creating washer model: " (.getMessage e))})))

;; Register this model with the registry
(registry/register-model
 "washer"
 {:schema-fn schema
  :create-fn create})