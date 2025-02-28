(ns cad-os.formats
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [cad-os.filename :as filename]
            [clojure.string :as str]))

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

(defn convert-g-to-stl
  "Convert a .g file to .stl format using the g-stl command-line tool."
  ([file-path objects]
   (convert-g-to-stl file-path objects {}))

  ([file-path objects options]
   (let [base-path (filename/base-filename file-path)
         g-file (filename/with-extension base-path :g)
         output-file (or (:output-file options) (filename/with-extension base-path :stl))

         ; Build command arguments
         cmd-args (cond-> []
                    (:binary options)       (conj "-b")
                    (:verbose options)      (conj "-v")
                    (:invert options)       (conj "-i")
                    (:8bit-binary options)  (conj "-8")
                    (:colors options)       (conj (str "-x" (or (:colors-level options) "")))
                    (:no-colors options)    (conj (str "-X" (or (:no-colors-level options) "")))
                    (:abs-tess-tol options) (conj "-a" (str (:abs-tess-tol options)))
                    (:rel-tess-tol options) (conj "-r" (str (:rel-tess-tol options)))
                    (:norm-tess-tol options) (conj "-n" (str (:norm-tess-tol options)))
                    (:dist-calc-tol options) (conj "-D" (str (:dist-calc-tol options)))
                    true                    (conj "-o" output-file)
                    true                    (conj g-file)
                    true                    (concat objects))

         ; Execute command
         _ (println "Executing:" (str/join " " (cons "g-stl" cmd-args)))
         result (apply sh "g-stl" cmd-args)]

     ; Return result
     (if (zero? (:exit result))
       (do
         (println "Command completed successfully, checking for STL file:" output-file)
         (if (wait-for-file output-file 30 200)
           (do
             (println "STL file confirmed to exist:" output-file)
             {:status "success"
              :message (str "Created STL file: " output-file)
              :output (:out result)
              :file output-file})
           {:status "error"
            :message (str "STL file was not created despite successful command execution: " output-file)
            :output (:out result)
            :error "File not found after timeout"}))
       {:status "error"
        :message (str "Failed to convert file: " g-file)
        :error (:err result)
        :exit-code (:exit result)}))))

(defn convert-g-to-step
  "Convert a .g file to .step format using the g-step command-line tool."
  ([file-path]
   (convert-g-to-step file-path {}))

  ([file-path options]
   (let [base-path (filename/base-filename file-path)
         g-file (filename/with-extension base-path :g)
         output-file (or (:output-file options) (filename/with-extension base-path :step))

         ; Build command arguments
         cmd-args ["-o" output-file g-file]

         ; Execute command
         _ (println "Executing:" (str/join " " (cons "g-step" cmd-args)))
         result (apply sh "g-step" cmd-args)]

     ; Return result
     (if (zero? (:exit result))
       (do
         (println "Command completed successfully, checking for STEP file:" output-file)
         (if (wait-for-file output-file 30 200)
           (do
             (println "STEP file confirmed to exist:" output-file)
             {:status "success"
              :message (str "Created STEP file: " output-file)
              :output (:out result)
              :file output-file})
           {:status "error"
            :message (str "STEP file was not created despite successful command execution: " output-file)
            :output (:out result)
            :error "File not found after timeout"}))
       {:status "error"
        :message (str "Failed to convert file: " g-file)
        :error (:err result)
        :exit-code (:exit result)}))))

;; Ensure a file exists in the specified format
(defn ensure-format
  "Ensure a file exists in the specified format, converting if necessary"
  [file-name format]
  (let [base-name (filename/base-filename file-name)
        model-type (filename/extract-model-type file-name)
        target-file (filename/with-extension base-name format)]

    (println "Ensuring format" format "for file" base-name "model type" model-type)

    ;; Check if the target file exists
    (if (.exists (io/file target-file))
      {:status "success" :file target-file}

      ;; If not, check if g file exists and convert
      (let [g-file (filename/with-extension base-name :g)]
        (if (.exists (io/file g-file))
          (case format
            :g {:status "success" :file g-file}
            :obj (do
                   (println "Converting G to OBJ:" base-name model-type)
                   (let [result (require 'cad-os.obj)
                         obj-fn (resolve 'cad-os.obj/convert-g-to-obj)]
                     (obj-fn base-name [model-type] {:mesh true, :abs-tess-tol 0.01})))
            :stl (convert-g-to-stl base-name [model-type] {:abs-tess-tol 0.01})
            :step (convert-g-to-step base-name))
          {:status "error"
           :message (str "G file not found for conversion: " g-file)})))))