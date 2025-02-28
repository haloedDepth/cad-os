(ns cad-os.models.core
  (:require [cad-os.commands :as commands]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [cad-os.obj :as obj]
            [cad-os.models.schema :as schema]
            [cad-os.formats :as formats]
            [cad-os.filename :as filename]
            [clojure.java.io :as io]))

(def mged-path "/usr/brlcad/rel-7.40.3/bin/mged")

(defn wait-for-file
  "Wait for a file to exist with timeout"
  [file-path max-attempts delay-ms]
  (loop [attempts 0]
    (let [file (io/file file-path)]
      (if (.exists file)
        true
        (if (>= attempts max-attempts)
          false
          (do
            (Thread/sleep delay-ms)
            (recur (inc attempts))))))))

(defn create-g-file
  "Create a .g file from a list of commands"
  [file-name commands]
  (let [g-file-path (filename/with-extension file-name :g)
        joined (s/join ";" commands)]
    (println "Creating model:" file-name)
    (println "Running:" mged-path "-c" g-file-path joined)
    (let [shell-result (sh mged-path "-c" g-file-path joined)]
      (assoc shell-result :file g-file-path))))

(defn create-model
  "Create a model with optional conversion to requested formats
   
   Parameters:
   - file-name: Base name for the model files (without extension)
   - model-name: Name of the model inside the .g file
   - commands: List of commands to create the model
   - formats: Set of formats to generate (:g, :obj, :stl, :step)"
  [file-name model-name commands & {:keys [formats] :or {formats #{:g}}}]
  (try
    ;; Create the .g file (always required as base format)
    (let [g-file-path (filename/with-extension file-name :g)
          g-result (create-g-file file-name commands)
          g-file-exists (wait-for-file g-file-path 30 100)]

      (if-not g-file-exists
        ;; G file creation failed
        {:status "error"
         :message "G file was not created in time"
         :g-result g-result}

        ;; G file created successfully, now process additional formats if requested
        (let [result {:status "success"
                      :file-name file-name
                      :g-result g-result
                      :g-path g-file-path}

              ;; Process OBJ format if requested
              result-with-obj (if (contains? formats :obj)
                                (let [obj-file-path (filename/with-extension file-name :obj)
                                      obj-result (obj/convert-g-to-obj file-name [model-name]
                                                                       {:mesh true, :verbose true, :abs-tess-tol 0.01})
                                      obj-file-exists (wait-for-file obj-file-path 30 100)]
                                  (assoc result
                                         :obj-result obj-result
                                         :obj-path (if obj-file-exists obj-file-path nil)))
                                result)

              ;; Process STL format if requested
              result-with-stl (if (contains? formats :stl)
                                (let [stl-file-path (filename/with-extension file-name :stl)
                                      stl-result (formats/convert-g-to-stl file-name [model-name] {:abs-tess-tol 0.01})]
                                  (assoc result-with-obj
                                         :stl-result stl-result
                                         :stl-path (when (= (:status stl-result) "success") stl-file-path)))
                                result-with-obj)

              ;; Process STEP format if requested
              final-result (if (contains? formats :step)
                             (let [step-file-path (filename/with-extension file-name :step)
                                   step-result (formats/convert-g-to-step file-name)]
                               (assoc result-with-stl
                                      :step-result step-result
                                      :step-path (when (= (:status step-result) "success") step-file-path)))
                             result-with-stl)]

          final-result)))
    (catch Exception e
      (println "Exception in model creation:" (.getMessage e))
      (.printStackTrace e)
      {:status "error"
       :message (str "Error creating model: " (.getMessage e))})))

;; Generic parameter handling

(defn normalize-params
  "Convert parameter keys to a consistent format for processing"
  [params]
  (reduce-kv (fn [m k v]
               (let [key-str (cond
                               (keyword? k) (name k)
                               (string? k) k
                               :else (str k))
                     normalized-key (keyword (s/replace key-str "_" "-"))]
                 (assoc m normalized-key v)))
             {} params))

(defn apply-defaults
  "Apply default values from schema for missing parameters"
  [params schema]
  (let [normalized-params (normalize-params params)]
    (reduce (fn [acc param-spec]
              (let [param-name (keyword (:name param-spec))
                    default-value (:default param-spec)]
                (if (and (not (contains? acc param-name))
                         (contains? param-spec :default))
                  (assoc acc param-name default-value)
                  acc)))
            normalized-params
            (:parameters schema))))

(defn convert-param-types
  "Convert parameter values to their correct types according to schema"
  [params schema]
  (reduce (fn [acc param-spec]
            (let [param-name (keyword (:name param-spec))
                  param-value (get acc param-name)
                  param-type (:type param-spec)]
              (if (and param-value (= param-type "number") (or (string? param-value) (number? param-value)))
                (try
                  (let [numeric-value (if (number? param-value)
                                        param-value
                                        (Double/parseDouble (str param-value)))]
                    (assoc acc param-name numeric-value))
                  (catch Exception e
                    acc))
                acc)))
          params
          (:parameters schema)))

;; Generic model creation function
(defn create-model-from-generator
  "Create a model using a command generator function"
  [model-type params command-generator & {:keys [formats] :or {formats #{:g}}}]
  (try
    (println "Creating" model-type "with params:" params "for formats:" formats)

    ;; Generate the file name using our utility function
    (let [file-name (filename/generate-model-filename model-type params)

          ;; Generate commands for the model
          commands (command-generator params)]

      (println "Using file name:" file-name)
      (println "Generated commands:" commands)

      ;; Create the actual model with requested formats
      (create-model file-name model-type commands :formats formats))

    (catch Exception e
      (println "Exception in" model-type "creation:" (.getMessage e))
      (.printStackTrace e)
      {:status "error", :message (str "Error creating " model-type " model: " (.getMessage e))})))