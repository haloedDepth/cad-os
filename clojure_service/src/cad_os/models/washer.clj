(ns cad-os.models.washer
  (:require [cad-os.models.core :as model-core]
            [cad-os.commands :as commands]))

(defn parse-params
  "Parse and validate washer parameters from a request map"
  [params]
  (let [get-param (fn [key]
                    (or (get params (keyword key))
                        (get params (str key))
                        (get params (symbol key))))

        outer-diameter-str (str (get-param "outer-diameter"))
        inner-diameter-str (str (get-param "inner-diameter"))
        thickness-str (str (get-param "thickness"))]

    ;; Check if any required parameters are missing
    (if (some #(or (nil? %) (= "nil" %) (empty? %))
              [outer-diameter-str inner-diameter-str thickness-str])
      {:valid false
       :message "Missing required parameters: outer-diameter, inner-diameter, thickness"}

      ;; Parse values
      (try
        (let [outer-d (Double/parseDouble outer-diameter-str)
              inner-d (Double/parseDouble inner-diameter-str)
              thick (Double/parseDouble thickness-str)]

          ;; Validate values
          (cond
            (<= outer-d 0) {:valid false, :message "Outer diameter must be positive"}
            (<= inner-d 0) {:valid false, :message "Inner diameter must be positive"}
            (<= thick 0) {:valid false, :message "Thickness must be positive"}
            (>= inner-d outer-d) {:valid false, :message "Inner diameter must be less than outer diameter"}
            :else {:valid true
                   :params {:outer-diameter outer-d
                            :inner-diameter inner-d
                            :thickness thick}}))
        (catch Exception e
          {:valid false
           :message (str "Error parsing parameters: " (.getMessage e))})))))

(defn generate-commands
  "Generate commands to create a washer model"
  [outer-diameter inner-diameter thickness]
  [(commands/insert-right-circular-cylinder "outer" 0 0 0 0 0 thickness (/ outer-diameter 2))
   (commands/insert-right-circular-cylinder "inner" 0 0 0 0 0 thickness (/ inner-diameter 2))
   (commands/subtraction "washer" "outer" "inner")])

(defn get-file-name
  "Generate a file name for a washer based on its parameters"
  [params]
  (let [parsed (parse-params params)]
    (if (:valid parsed)
      (let [{:keys [outer-diameter inner-diameter thickness]} (:params parsed)]
        (str "washer_" outer-diameter "_" inner-diameter "_" thickness))
      nil)))

(defn create
  "Create a washer model from request parameters"
  [params]
  (let [parsed (parse-params params)]
    (if (:valid parsed)
      (let [{:keys [outer-diameter inner-diameter thickness]} (:params parsed)
            file-name (get-file-name params)
            commands (generate-commands outer-diameter inner-diameter thickness)]
        (model-core/create-model file-name "washer" commands))
      {:status "error", :message (:message parsed)})))