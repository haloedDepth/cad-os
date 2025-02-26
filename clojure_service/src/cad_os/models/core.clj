(ns cad-os.models.core
  (:require [cad-os.commands :as commands]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [cad-os.obj :as obj]
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

;; Add these functions to cad-os.models.core namespace

(defn parse-param
  "Parse a parameter value from a params map"
  [params param-name]
  (let [variants [param-name
                  (keyword param-name)
                  (str param-name)
                  (symbol param-name)
                  (clojure.string/replace param-name "-" "_")
                  (keyword (clojure.string/replace param-name "-" "_"))
                  (clojure.string/replace param-name "_" "-")
                  (keyword (clojure.string/replace param-name "_" "-"))]
        param-value (some #(get params %) variants)]
    (println "Parsing parameter" param-name "from params" params "-> value:" param-value)
    (when (and param-value
               (not= param-value "nil")
               (not= param-value ""))
      (str param-value))))

(defn parse-numeric-params
  "Parse and validate numeric parameters from a params map"
  [params param-specs]
  (println "Parsing numeric parameters from:" params)
  (println "With specs:" param-specs)
  (try
    (let [parsed-params (reduce (fn [result {:keys [name min max default]}]
                                  (let [snake-case (clojure.string/replace name "-" "_")
                                        kebab-case (clojure.string/replace name "_" "-")
                                        value-raw (or (get params (keyword name))
                                                      (get params name)
                                                      (get params (keyword snake-case))
                                                      (get params snake-case)
                                                      (get params (keyword kebab-case))
                                                      (get params kebab-case))]
                                    (println (str "For parameter " name ": found value " value-raw))
                                    (if (nil? value-raw)
                                      (if default
                                        (do
                                          (println (str "Using default value for " name ": " default))
                                          (assoc result (keyword name) default))
                                        (reduced {:valid false
                                                  :message (str "Missing required parameter: " name)}))
                                      (let [value-str (str value-raw)
                                            _ (println (str "Converting " value-str " to number"))
                                            value (try
                                                    (Double/parseDouble value-str)
                                                    (catch Exception e
                                                      (println "Parse error:" (.getMessage e))
                                                      nil))]
                                        (cond
                                          (nil? value)
                                          (reduced {:valid false
                                                    :message (str "Could not parse " name " as a number: " value-str)})

                                          (and min (< value min))
                                          (reduced {:valid false
                                                    :message (str name " must be at least " min)})

                                          (and max (> value max))
                                          (reduced {:valid false
                                                    :message (str name " must be at most " max)})

                                          :else
                                          (do
                                            (println (str "Parameter " name " = " value " (validated)"))
                                            (assoc result (keyword name) value)))))))
                                {} param-specs)]
      (println "Final parsed params:" parsed-params)
      (if (and (map? parsed-params) (contains? parsed-params :valid))
        parsed-params  ; This is already an error result
        {:valid true :params parsed-params}))
    (catch Exception e
      (println "Exception in parse-numeric-params:" (.getMessage e))
      (.printStackTrace e)
      {:valid false
       :message (str "Error parsing parameters: " (.getMessage e))})))