(ns cad-os.formats
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [cad-os.filename :as filename]
            [clojure.string :as str]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

(defn wait-for-file
  "Wait for a file to exist with timeout"
  [file-path max-attempts delay-ms]
  ((:debug log) "Waiting for file" {:path file-path :max-attempts max-attempts})
  (loop [attempts 0]
    (let [file (io/file file-path)]
      (if (.exists file)
        (do
          ((:debug log) "File exists" {:path file-path :attempts attempts})
          true)
        (if (>= attempts max-attempts)
          (do
            ((:warn log) "Timeout waiting for file" {:path file-path :attempts attempts})
            false)
          (do
            (Thread/sleep delay-ms)
            (recur (inc attempts))))))))

(defn convert-g-to-stl
  "Convert a .g file to .stl format using the g-stl command-line tool."
  ([file-path objects]
   (convert-g-to-stl file-path objects {}))

  ([file-path objects options]
   ((:info log) "Converting G to STL" {:file file-path :objects objects})
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
         _ ((:debug log) "Executing command" {:command "g-stl" :args cmd-args})
         result (apply sh "g-stl" cmd-args)]

     ; Return result
     (if (zero? (:exit result))
       (do
         ((:debug log) "Command completed successfully" {:output-file output-file})
         (if (wait-for-file output-file 30 200)
           (do
             ((:info log) "STL file created successfully" {:file output-file})
             {:status "success"
              :message (str "Created STL file: " output-file)
              :output (:out result)
              :file output-file})
           (do
             ((:error log) "STL file was not created despite successful command"
                           {:output-file output-file :command-output (:out result)})
             {:status "error"
              :message (str "STL file was not created despite successful command execution: " output-file)
              :output (:out result)
              :error "File not found after timeout"})))
       (do
         ((:error log) "Failed to convert file"
                       {:input-file g-file :exit-code (:exit result) :error (:err result)})
         {:status "error"
          :message (str "Failed to convert file: " g-file)
          :error (:err result)
          :exit-code (:exit result)})))))

(defn convert-g-to-step
  "Convert a .g file to .step format using the g-step command-line tool."
  ([file-path]
   (convert-g-to-step file-path {}))

  ([file-path options]
   ((:info log) "Converting G to STEP" {:file file-path})
   (let [base-path (filename/base-filename file-path)
         g-file (filename/with-extension base-path :g)
         output-file (or (:output-file options) (filename/with-extension base-path :step))

         ; Build command arguments
         cmd-args ["-o" output-file g-file]

         ; Execute command
         _ ((:debug log) "Executing command" {:command "g-step" :args cmd-args})
         result (apply sh "g-step" cmd-args)]

     ; Return result
     (if (zero? (:exit result))
       (do
         ((:debug log) "Command completed successfully" {:output-file output-file})
         (if (wait-for-file output-file 30 200)
           (do
             ((:info log) "STEP file created successfully" {:file output-file})
             {:status "success"
              :message (str "Created STEP file: " output-file)
              :output (:out result)
              :file output-file})
           (do
             ((:error log) "STEP file was not created despite successful command"
                           {:output-file output-file :command-output (:out result)})
             {:status "error"
              :message (str "STEP file was not created despite successful command execution: " output-file)
              :output (:out result)
              :error "File not found after timeout"})))
       (do
         ((:error log) "Failed to convert file"
                       {:input-file g-file :exit-code (:exit result) :error (:err result)})
         {:status "error"
          :message (str "Failed to convert file: " g-file)
          :error (:err result)
          :exit-code (:exit result)})))))

;; Ensure a file exists in the specified format
(defn ensure-format
  "Ensure a file exists in the specified format, converting if necessary"
  [file-name format]
  (let [base-name (filename/base-filename file-name)
        model-type (filename/extract-model-type file-name)
        target-file (filename/with-extension base-name format)]

    ((:info log) "Ensuring format" {:file base-name :format format :model-type model-type})

    ;; Check if the target file exists
    (if (.exists (io/file target-file))
      (do
        ((:debug log) "Target file already exists" {:file target-file})
        {:status "success" :file target-file})

      ;; If not, check if g file exists and convert
      (let [g-file (filename/with-extension base-name :g)]
        (if (.exists (io/file g-file))
          (case format
            :g (do
                 ((:debug log) "Using existing G file" {:file g-file})
                 {:status "success" :file g-file})
            :obj (do
                   ((:info log) "Converting G to OBJ" {:file base-name :model-type model-type})
                   (let [result (require 'cad-os.obj)
                         obj-fn (resolve 'cad-os.obj/convert-g-to-obj)]
                     (obj-fn base-name [model-type] {:mesh true, :abs-tess-tol 0.01})))
            :stl (convert-g-to-stl base-name [model-type] {:abs-tess-tol 0.01})
            :step (convert-g-to-step base-name))
          (do
            ((:error log) "G file not found for conversion" {:file g-file})
            {:status "error"
             :message (str "G file not found for conversion: " g-file)}))))))