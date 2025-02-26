(ns cad-os.models.core
  (:require [cad-os.commands :as commands]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [cad-os.obj :as obj]
            [cad-os.models.schema :as schema]
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
  (let [joined (s/join ";" commands)]
    (println "Creating model:" file-name)
    (println "Running:" mged-path "-c" (str file-name ".g") joined)
    (let [shell-result (sh mged-path "-c" (str file-name ".g") joined)]
      (assoc shell-result :file (str file-name ".g")))))

(defn create-model
  "Create a model and convert it to OBJ format"
  [file-name model-name commands]
  (let [;; Create the .g file
        g-result (create-g-file file-name commands)
        g-file-path (str file-name ".g")

        ;; Wait for the .g file to exist
        g-file-exists (wait-for-file g-file-path 30 100)

        ;; Convert to OBJ if the .g file exists
        obj-result (if g-file-exists
                     (obj/convert-g-to-obj file-name [model-name]
                                           {:mesh true, :verbose true, :abs-tess-tol 0.01})
                     {:status "error" :message "G file was not created in time"})

        ;; Wait for the OBJ file to exist
        obj-file-path (str file-name ".obj")
        obj-file-exists (wait-for-file obj-file-path 30 100)]

    (if obj-file-exists
      {:status "success"
       :file-name file-name
       :g-result g-result
       :obj-result obj-result
       :obj-path (get obj-result :file (str file-name ".obj"))}
      {:status "error"
       :message "OBJ file was not created in time"
       :g-result g-result
       :obj-result obj-result})))

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

(defn generate-file-name
  "Generate a standard file name based on model type and parameters"
  [model-type params]
  (let [param-values (vals (select-keys params (keys params)))
        param-str (s/join "_" param-values)]
    (str model-type "_" param-str)))

;; Generic model creation function
(defn create-model-from-generator
  "Create a model using a command generator function"
  [model-type params command-generator]
  (try
    (println "Creating" model-type "with params:" params)

    ;; Generate the file name
    (let [file-name (generate-file-name model-type params)

          ;; Generate commands for the model
          commands (command-generator params)]

      (println "Using file name:" file-name)
      (println "Generated commands:" commands)

      ;; Create the actual model
      (create-model file-name model-type commands))

    (catch Exception e
      (println "Exception in" model-type "creation:" (.getMessage e))
      (.printStackTrace e)
      {:status "error", :message (str "Error creating " model-type " model: " (.getMessage e))})))