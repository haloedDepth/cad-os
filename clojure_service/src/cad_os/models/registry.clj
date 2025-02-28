(ns cad-os.models.registry
  (:require [clojure.string :as str]
            [cad-os.models.core :as core]
            [cad-os.models.schema :as schema-utils]
            [cad-os.utils.logger :as logger]
            [cad-os.utils.errors :as errors]))

;; Initialize logger
(def log (logger/get-logger))

;; Map of model types to their factory functions
(def model-registry (atom {}))

;; Register a model type with its factory functions
(defn register-model
  "Register a model type with its schema and command-generator function"
  [model-type schema command-generator]
  (let [schema-validation (schema-utils/validate-schema schema)]
    (if (:valid schema-validation)
      (do
        ((:info log) "Registering model" {:type model-type})
        (swap! model-registry assoc model-type
               {:schema schema
                :command-generator command-generator})
        true)
      (do
        ((:error log) "Invalid schema for model"
                      {:type model-type
                       :error (:message schema-validation)})
        false))))

;; Get all registered model types
(defn get-model-types
  "Get all registered model types"
  []
  ((:debug log) "Getting all model types")
  (keys @model-registry))

;; Get schema for a specific model type
(defn get-model-schema
  "Get schema for a specific model type"
  [model-type]
  ((:debug log) "Getting schema for model type" {:type model-type})
  (when-let [model-info (@model-registry model-type)]
    (schema-utils/enrich-schema (:schema model-info))))

;; Get all schemas (for frontend loading)
(defn get-all-schemas
  "Get all model schemas with validation info for frontend use"
  []
  ((:info log) "Getting all schemas from registry")
  (let [result (reduce-kv (fn [result model-type model-info]
                            ((:debug log) "Processing schema for model type" {:type model-type})
                            (let [enriched (schema-utils/enrich-schema (:schema model-info))]
                              (assoc result model-type enriched)))
                          {}
                          @model-registry)]
    ((:debug log) "All schemas prepared" {:count (count result)})
    result))

;; Create a model of the specified type with the given parameters
(defn create-model
  "Create a model of the specified type with the given parameters."
  [model-type params & {:keys [formats] :or {formats #{:g :obj}}}]
  ((:info log) "Creating model" {:type model-type :params params :formats formats})
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

      (do
        ((:warn log) "Unknown model type" {:type model-type})
        {:status "error"
         :message (str "Unknown model type: " model-type)}))
    (catch Exception e
      ((:error log) "Error creating model" {:type model-type :params params} e)
      {:status "error"
       :message (str "Error creating model: " (.getMessage e))})))