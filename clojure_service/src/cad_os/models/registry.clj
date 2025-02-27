(ns cad-os.models.registry
  (:require [clojure.string :as str]
            [cad-os.models.core :as core]
            [cad-os.models.schema :as schema-utils]))

;; Map of model types to their factory functions
(def model-registry (atom {}))

;; Register a model type with its factory functions
(defn register-model
  "Register a model type with its schema and command-generator function"
  [model-type schema command-generator]
  (let [schema-validation (schema-utils/validate-schema schema)]
    (if (:valid schema-validation)
      (do
        (println "Registering model:" model-type)
        (swap! model-registry assoc model-type
               {:schema schema
                :command-generator command-generator})
        true)
      (do
        (println "Invalid schema for" model-type ":" (:message schema-validation))
        false))))

;; Get all registered model types
(defn get-model-types
  "Get all registered model types"
  []
  (keys @model-registry))

;; Get schema for a specific model type
(defn get-model-schema
  "Get schema for a specific model type"
  [model-type]
  (when-let [model-info (@model-registry model-type)]
    (schema-utils/enrich-schema (:schema model-info))))

;; Get all schemas (for frontend loading)
(defn get-all-schemas
  "Get all model schemas with validation info for frontend use"
  []
  (println "Getting all schemas from registry")
  (let [result (reduce-kv (fn [result model-type model-info]
                            (println "Processing schema for" model-type)
                            (let [enriched (schema-utils/enrich-schema (:schema model-info))]
                              (println "Enriched schema:" enriched)
                              (assoc result model-type enriched)))
                          {}
                          @model-registry)]
    (println "All schemas result:" result)
    result))

;; Create a model of the specified type with the given parameters
(defn create-model
  "Create a model of the specified type with the given parameters.
   
   Parameters:
   - model-type: Type of model to create
   - params: Parameters for the model
   - formats: Set of formats to generate - default is both .g and .obj for web interface"
  [model-type params & {:keys [formats] :or {formats #{:g :obj}}}]
  (println "Registry creating model of type:" model-type "with params:" params "for formats:" formats)
  (try
    (if-let [model-info (@model-registry model-type)]
      (let [model-schema (:schema model-info)
            command-generator (:command-generator model-info)

            ;; Normalize and apply defaults - skip validation completely
            processed-params (-> params
                                 (core/normalize-params)
                                 (core/apply-defaults model-schema)
                                 (core/convert-param-types model-schema))]

        ;; Create the model with requested formats
        (core/create-model-from-generator model-type processed-params command-generator :formats formats))

      {:status "error"
       :message (str "Unknown model type: " model-type)})
    (catch Exception e
      (println "Exception in registry create-model:" (.getMessage e))
      (.printStackTrace e)
      {:status "error"
       :message (str "Error creating model: " (.getMessage e))})))