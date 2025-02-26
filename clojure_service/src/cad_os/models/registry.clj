(ns cad-os.models.registry
  (:require [clojure.string :as str]))

;; Map of model types to their factory functions
(def model-registry (atom {}))

;; Register a model type with its factory functions
(defn register-model
  "Register a model type with its factory functions"
  [model-type {:keys [schema-fn create-fn] :as model-fns}]
  (swap! model-registry assoc model-type model-fns))

;; Get all registered model types
(defn get-model-types
  "Get all registered model types"
  []
  (keys @model-registry))

;; Get schema for a specific model type
(defn get-model-schema
  "Get schema for a specific model type"
  [model-type]
  (when-let [schema-fn (:schema-fn (@model-registry model-type))]
    (schema-fn)))

;; Create a model of the specified type with the given parameters
(defn create-model
  "Create a model of the specified type with the given parameters"
  [model-type params]
  (println "Registry creating model of type:" model-type "with params:" params)
  (try
    (if-let [create-fn (:create-fn (@model-registry model-type))]
      (let [result (create-fn params)]
        (println "Model creation result:" result)
        result)
      {:status "error"
       :message (str "Unknown model type: " model-type)})
    (catch Exception e
      (println "Exception in registry create-model:" (.getMessage e))
      (.printStackTrace e)
      {:status "error"
       :message (str "Error creating model: " (.getMessage e))})))