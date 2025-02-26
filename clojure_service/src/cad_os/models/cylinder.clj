(ns cad-os.models.cylinder
  (:require [cad-os.models.core :as model-core]
            [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; Schema function to describe the model parameters
(defn schema
  "Return a schema description for cylinder parameters"
  []
  {:name "Cylinder"
   :description "A simple cylinder with specified radius and height"
   :parameters
   [{:name "radius"
     :type "number"
     :description "Radius of the cylinder"
     :default 5.0
     :min 0.1}
    {:name "height"
     :type "number"
     :description "Height of the cylinder"
     :default 10.0
     :min 0.1}]})

;; Parse and validate cylinder parameters
(defn parse-params
  "Parse and validate cylinder parameters from a request map"
  [params]
  (println "Cylinder parse-params called with:" params)
  (let [param-specs [{:name "radius" :min 0.1}
                     {:name "height" :min 0.1}]
        result (model-core/parse-numeric-params params param-specs)]
    (println "Parse result:" result)
    result))

;; Generate commands to create a cylinder model
(defn generate-commands
  "Generate commands to create a cylinder model"
  [radius height]
  (println "Generating cylinder commands with: radius=" radius "height=" height)
  [(commands/insert-right-circular-cylinder "cylinder" 0 0 0 0 0 height radius)])

;; Get file name for a cylinder based on its parameters
(defn get-file-name
  "Generate a file name for a cylinder based on its parameters"
  [params]
  (let [parsed (parse-params params)]
    (if (:valid parsed)
      (let [{:keys [radius height]} (:params parsed)]
        (str "cylinder_" radius "_" height))
      nil)))

;; Create a cylinder model from request parameters
(defn create
  "Create a cylinder model from request parameters"
  [params]
  (println "Creating cylinder with params:" params)
  (try
    (let [parsed (parse-params params)]
      (println "Parsed params result:" parsed)
      (if (:valid parsed)
        (let [{:keys [radius height]} (:params parsed)
              file-name (get-file-name params)
              _ (println "Using file name:" file-name)
              commands (generate-commands radius height)]
          (println "Generated commands:" commands)
          (model-core/create-model file-name "cylinder" commands))
        {:status "error", :message (or (:message parsed) "Invalid parameters")}))
    (catch Exception e
      (println "Exception in cylinder creation:" (.getMessage e))
      (.printStackTrace e)
      {:status "error", :message (str "Error creating cylinder model: " (.getMessage e))})))

;; Register this model with the registry
(registry/register-model
 "cylinder"
 {:schema-fn schema
  :create-fn create})