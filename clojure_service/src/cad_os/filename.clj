(ns cad-os.filename
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cad-os.utils.logger :as logger])
  (:import [java.security MessageDigest]
           [java.util Base64]))

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

(defn with-suffix
  "Add a suffix to a filename before the extension"
  [filename suffix]
  (let [base (base-filename filename)]
    ((:debug log) "Adding suffix to filename"
                  {:filename base
                   :suffix suffix})
    (str base "_" suffix)))

(defn with-suffix-and-extension
  "Add a suffix to a filename and ensure it has the specified extension"
  [filename suffix format]
  (let [base (base-filename filename)
        ext (get-extension format)]
    ((:debug log) "Adding suffix and extension to filename"
                  {:filename base
                   :suffix suffix
                   :format format
                   :extension ext})
    (str base "_" suffix "." ext)))

(defn extract-model-type
  "Extract model type from a filename"
  [filename]
  (let [base (base-filename filename)
        parts (str/split base #"-")]
    ((:debug log) "Extracting model type from filename"
                  {:filename base
                   :model-type (first parts)})
    (first parts)))

;; New hash generation function
(defn generate-hash-from-params
  "Generate a short hash from model type and parameters for unique identification"
  [model-type params]
  (let [;; Sort parameters for consistent ordering, joining key=value pairs
        sorted-params (sort (map (fn [[k v]] (str (name k) "=" v)) params))
        ;; Create a string representation of model type and parameters
        param-str (str model-type ":" (str/join ";" sorted-params))
        ;; Generate SHA-256 hash
        md (MessageDigest/getInstance "SHA-256")
        hash-bytes (.digest md (.getBytes param-str "UTF-8"))
        ;; Encode as base64 and take first 10 chars (for shorter filenames)
        hash-encoded (-> (Base64/getEncoder)
                         (.encodeToString hash-bytes)
                         (str/replace #"[/\+=]" "_") ; Replace problematic chars
                         (subs 0 10))]
    ((:debug log) "Generated hash for parameters"
                  {:model-type model-type :hash hash-encoded})
    hash-encoded))

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

;; Updated filename generation to use hash-based approach
(defn generate-model-filename
  "Generate a standard filename for a model based on its type and parameters.
   Returns the base filename without extension.
   Uses a hash of parameters to create shorter filenames."
  [model-type params]
  ((:debug log) "Generating model filename"
                {:type model-type
                 :param-count (count params)})

  ;; Filter out position parameters as these are only relevant for assemblies
  (let [filtered-params (into {} (remove (fn [[k _]]
                                           (let [key-name (name k)]
                                             (or (str/starts-with? key-name "position")
                                                 (str/starts-with? key-name "position-"))))
                                         params))

        ;; Generate hash for the parameters
        param-hash (generate-hash-from-params model-type filtered-params)

        ;; Create a parameter mapping filename for debugging/reference
        param-map-str (str/join "_" (map (fn [[k v]]
                                           (str (name k) "=" (encode-param-value v)))
                                         (sort filtered-params)))

        ;; Store this mapping in a metadata file if needed for debugging
        ;; (could be implemented here)

        ;; Construct the shorter filename with hash
        filename (str model-type "-" param-hash)]

    ((:info log) "Generated filename" {:filename filename})
    filename))

;; Keep existing parse-params-from-filename for legacy support
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