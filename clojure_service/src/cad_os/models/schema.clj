(ns cad-os.models.schema
  (:require [clojure.string :as str]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Parse an expression into a function
(defn parse-expression
  "Parse a validation expression into a function that can be used to validate parameters"
  [expr param-names]
  (try
    ((:debug log) "Parsing expression" {:expr expr :param-names param-names})

    ;; Create a string representation of the function that will validate the expression
    ;; Using let bindings for all parameters to avoid issues with kebab-case names
    (let [let-bindings (mapcat (fn [param]
                                 [(symbol (str "param-" (str/replace param #"-" "_")))
                                  `(get ~'params ~(keyword param))])
                               param-names)

          ;; Replace parameter names in the expression with their corresponding let bindings
          expr-with-bindings (reduce (fn [expr-str param]
                                       (let [param-replacement (str "param-" (str/replace param #"-" "_"))
                                             regex (re-pattern (str "\\b" param "\\b"))]
                                         (str/replace expr-str regex param-replacement)))
                                     expr
                                     param-names)

          ;; Create the actual function
          expr-fn (eval
                   `(fn [~'params]
                      (let [~@let-bindings]
                        ~(read-string expr-with-bindings))))]
      {:valid true
       :fn expr-fn})
    (catch Exception e
      ((:error log) "Error parsing expression" {:expr expr :error (.getMessage e)} e)
      {:valid false
       :message (str "Invalid expression: " expr ". Error: " (.getMessage e))})))

;; Validate parameters using expressions
(defn validate-with-expressions
  "Validate parameters using a list of expression rules"
  [params rules param-names]
  ;; Always return valid - validation is now handled by frontend
  {:valid true})

;; Validate parameter types
(defn validate-param-types
  "Validate parameter types"
  [params schema]
  (let [param-specs (get schema :parameters [])]
    ((:debug log) "Validating parameter types" {:param-count (count params)})
    (loop [specs param-specs
           errors []]
      (if (empty? specs)
        (if (empty? errors)
          {:valid true}
          (do
            ((:warn log) "Parameter type validation failed" {:errors errors})
            {:valid false
             :errors errors}))
        (let [spec (first specs)
              param-name (:name spec)
              param-value (get params (keyword param-name))
              param-type (:type spec)
              type-error (cond
                           (and (= param-type "number") param-value
                                (not (number? param-value)))
                           (str param-name " must be a number")

                           (and (= param-type "number") (nil? param-value)
                                (not (contains? spec :default)))
                           (str param-name " is required")

                           :else nil)]
          (if type-error
            (recur (rest specs) (conj errors type-error))
            (recur (rest specs) errors)))))))

;; Validate a schema definition
(defn validate-schema
  "Validate a schema definition for correctness"
  [schema]
  ((:debug log) "Validating schema" {:name (:name schema)})
  (let [name (:name schema)
        description (:description schema)
        parameters (:parameters schema)
        validation-rules (:validation-rules schema)]

    (cond
      (not name)
      (do
        ((:error log) "Schema validation failed: missing name")
        {:valid false :message "Schema must have a name"})

      (not (vector? parameters))
      (do
        ((:error log) "Schema validation failed: parameters not a vector")
        {:valid false :message "Parameters must be a vector"})

      (not (every? (fn [p] (and (:name p) (:type p))) parameters))
      (do
        ((:error log) "Schema validation failed: parameters missing name or type")
        {:valid false :message "Each parameter must have a name and type"})

      (and validation-rules (not (vector? validation-rules)))
      (do
        ((:error log) "Schema validation failed: validation rules not a vector")
        {:valid false :message "Validation rules must be a vector"})

      (and validation-rules
           (not (every? (fn [r] (and (:expr r) (:message r))) validation-rules)))
      (do
        ((:error log) "Schema validation failed: validation rules missing expr or message")
        {:valid false :message "Each validation rule must have an expression and message"})

      :else
      (do
        ((:info log) "Schema validation successful" {:name name})
        {:valid true}))))

;; Filter visible parameters
(defn filter-visible-parameters
  "Filter out parameters marked as hidden"
  [parameters]
  (filter #(not (:hidden %)) parameters))

;; Enrich schema with frontend-specific info
(defn enrich-schema
  "Add frontend-specific validation info to schema"
  [schema]
  ((:debug log) "Enriching schema" {:name (:name schema)})
  (let [parameters (:parameters schema)
        visible-parameters (filter-visible-parameters parameters)
        param-names (mapv :name visible-parameters)
        frontend-schema (assoc schema
                               :param-names param-names
                               :parameters visible-parameters)]
    frontend-schema))