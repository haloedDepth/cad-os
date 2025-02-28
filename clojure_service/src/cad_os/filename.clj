(ns cad-os.filename
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

;; Define standard file extensions
(def format-extensions
  {:g "g"
   :obj "obj"
   :stl "stl"
   :step "stp"})

(defn get-extension
  "Get the file extension for a format"
  [format]
  (get format-extensions format))

(defn base-filename
  "Extract base filename (without extension)"
  [filename]
  (let [last-dot-index (str/last-index-of filename ".")]
    (if (and last-dot-index (> last-dot-index 0))
      (subs filename 0 last-dot-index)
      filename)))

(defn with-extension
  "Add the specified extension to a filename"
  [filename format]
  (let [base (base-filename filename)
        ext (get-extension format)]
    (str base "." ext)))

(defn extract-model-type
  "Extract model type from a filename"
  [filename]
  (let [base (base-filename filename)
        parts (str/split base #"-")]
    (first parts)))

(defn generate-model-filename
  "Generate a standard filename for a model based on its type and parameters.
   Returns the base filename without extension.
   Excludes position parameters as these are only relevant for assemblies.
   
   Parameters:
   - model-type: Type of the model (e.g., 'washer', 'cylinder')
   - params: Map of parameter values for the model"
  [model-type params]
  (let [;; Filter out position parameters
        filtered-params (into {} (remove (fn [[k _]]
                                           (let [key-name (name k)]
                                             (or (str/starts-with? key-name "position")
                                                 (str/starts-with? key-name "position-"))))
                                         params))
        ;; Sort parameters by name to ensure consistent ordering
        sorted-params (sort (map (fn [[k v]] [(name k) v]) filtered-params))
        ;; Format each parameter as key=value
        param-strs (map (fn [[k v]] (str k "=" v)) sorted-params)
        ;; Join with underscores
        param-str (str/join "_" param-strs)]
    (str model-type "-" param-str)))

(defn parse-params-from-filename
  "Extract parameters from a filename"
  [filename]
  (let [base (base-filename filename)
        parts (str/split base #"-")
        params-part (when (> (count parts) 1) (second parts))
        param-pairs (when params-part (str/split params-part #"_"))]
    (if param-pairs
      (reduce (fn [acc pair]
                (let [[k v] (str/split pair #"=")]
                  (assoc acc (keyword k) v)))
              {}
              param-pairs)
      {})))