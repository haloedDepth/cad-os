(ns cad-os.obj
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [cad-os.filename :as filename]
            [clojure.string :as str]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

(defn wait-for-command-completion
  "Wait for an external command to complete by checking for the existence of a file"
  [file-path max-attempts delay-ms]
  ((:debug log) "Waiting for file" {:path file-path :max-attempts max-attempts})
  (loop [attempts 0]
    (if (.exists (io/file file-path))
      (do
        ((:debug log) "File exists" {:path file-path :attempts attempts})
        true)
      (if (>= attempts max-attempts)
        (do
          ((:warn log) "Timeout waiting for file" {:path file-path :attempts attempts})
          false)
        (do
          (Thread/sleep delay-ms)
          (recur (inc attempts)))))))

(defn convert-g-to-obj
  "Convert a .g file to .obj format using the g-obj command-line tool."
  ([file-path objects]
   (convert-g-to-obj file-path objects {}))

  ([file-path objects options]
   ((:info log) "Converting G to OBJ" {:file file-path :objects objects})
   (let [base-path (filename/base-filename file-path)
         g-file (filename/with-extension base-path :g)
         output-file (or (:output-file options) (filename/with-extension base-path :obj))

         ; Build command arguments
         cmd-args (cond-> []
                    (:mesh options)         (conj "-m")
                    (:verbose options)      (conj "-v")
                    (:invert options)       (conj "-i")
                    (:units options)        (conj "-u")
                    (:colors options)       (conj (str "-x" (or (:colors-level options) "")))
                    (:no-colors options)    (conj (str "-X" (or (:no-colors-level options) "")))
                    (:abs-tess-tol options) (conj "-a" (str (:abs-tess-tol options)))
                    (:rel-tess-tol options) (conj "-r" (str (:rel-tess-tol options)))
                    (:norm-tess-tol options) (conj "-n" (str (:norm-tess-tol options)))
                    (:parallelism options)  (conj "-P" (str (:parallelism options)))
                    (:error-file options)   (conj "-e" (:error-file options))
                    (:dist-calc-tol options) (conj "-D" (str (:dist-calc-tol options)))
                    true                    (conj "-o" output-file)
                    true                    (conj g-file)
                    true                    (concat objects))

         ; Execute command
         _ ((:debug log) "Executing command" {:command "g-obj" :args cmd-args})
         result (apply sh "g-obj" cmd-args)]

     ; Wait for output file to exist
     (if (zero? (:exit result))
       (do
         ((:debug log) "Command completed successfully" {:output-file output-file})
         (if (wait-for-command-completion output-file 30 200)
           (do
             ((:info log) "OBJ file created successfully" {:file output-file})
             {:status "success"
              :message (str "Created OBJ file: " output-file)
              :output (:out result)
              :file output-file})
           (do
             ((:error log) "OBJ file was not created despite successful command"
                           {:output-file output-file :command-output (:out result)})
             {:status "error"
              :message (str "OBJ file was not created despite successful command execution: " output-file)
              :output (:out result)
              :error "File not found after timeout"})))
       (do
         ((:error log) "Failed to convert file"
                       {:input-file g-file :exit-code (:exit result) :error (:err result)})
         {:status "error"
          :message (str "Failed to convert file: " g-file)
          :error (:err result)
          :exit-code (:exit result)})))))