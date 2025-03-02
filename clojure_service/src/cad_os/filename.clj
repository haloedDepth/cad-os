(ns cad-os.filename
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Define standard file extensions
(def format-extensions
  {:g "g"
   :obj "obj"
   :stl "stl"
   :step "stp"})

(defn get-extension
  "Get the file extension for a format"
  [format]
  ((:debug log) "Getting extension for format" {:format format})
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
    ((:debug log) "Adding extension to filename"
                  {:filename base
                   :format format
                   :extension ext})
    (str base "." ext)))

(defn extract-model-type
  "Extract model type from a filename"
  [filename]
  (let [base (base-filename filename)
        parts (str/split base #"-")]
    ((:debug log) "Extracting model type from filename"
                  {:filename base
                   :model-type (first parts)})
    (first parts)))

(defn encode-param-value
  "Encode a parameter value for use in a filename.
   Replaces special characters like dots, slashes, etc."
  [value]
  (cond
    (nil? value) ""
    (number? value) (-> (str value)
                        (str/replace "." "_dot_"))
    (string? value) (-> value
                        (str/replace "." "_dot_")
                        (str/replace "/" "_slash_")
                        (str/replace "\\" "_backslash_"))
    :else (str value)))

(defn decode-param-value
  "Decode a parameter value from a filename string.
   Converts encoded special chars back to original form
   and attempts to parse numbers when appropriate."
  [value-str]
  (when value-str
    (let [decoded (-> value-str
                      (str/replace "_dot_" ".")
                      (str/replace "_slash_" "/")
                      (str/replace "_backslash_" "\\"))]
      ;; Try to parse as number if applicable
      (try
        (if (str/includes? decoded ".")
          (Double/parseDouble decoded)
          (Long/parseLong decoded))
        (catch NumberFormatException _
          decoded)))))

(defn generate-model-filename
  "Generate a standard filename for a model based on its type and parameters.
   Returns the base filename without extension.
   Excludes position parameters as these are only relevant for assemblies."
  [model-type params]
  ((:debug log) "Generating model filename"
                {:type model-type
                 :param-count (count params)})
  (let [;; Filter out position parameters
        filtered-params (into {} (remove (fn [[k _]]
                                           (let [key-name (name k)]
                                             (or (str/starts-with? key-name "position")
                                                 (str/starts-with? key-name "position-"))))
                                         params))
        ;; Sort parameters by name to ensure consistent ordering
        sorted-params (sort (map (fn [[k v]] [(name k) v]) filtered-params))
        ;; Format each parameter as key=value with encoded values
        param-strs (map (fn [[k v]] (str k "=" (encode-param-value v))) sorted-params)
        ;; Join with underscores
        param-str (str/join "_" param-strs)

        ;; Construct the full filename
        filename (str model-type "-" param-str)]

    ((:info log) "Generated filename" {:filename filename})
    filename))

(defn parse-params-from-filename
  "Extract parameters from a filename"
  [filename]
  (let [base (base-filename filename)
        parts (str/split base #"-")
        params-part (when (> (count parts) 1) (second parts))
        param-pairs (when params-part (str/split params-part #"_"))]

    ((:debug log) "Parsing parameters from filename" {:filename base})

    (if param-pairs
      (reduce (fn [acc pair]
                (if (str/includes? pair "=")
                  (let [[k v] (str/split pair #"=")]
                    (assoc acc (keyword k) (decode-param-value v)))
                  acc))
              {}
              param-pairs)
      {})))