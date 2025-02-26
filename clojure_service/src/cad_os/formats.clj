(ns cad-os.formats
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
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
   (let [file-path (str/replace file-path #"\.g$" "")  ; Remove .g extension if present
         g-file (str file-path ".g")                   ; Add .g extension
         output-file (or (:output-file options) (str file-path ".stl"))

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
   (let [file-path (str/replace file-path #"\.g$" "")  ; Remove .g extension if present
         g-file (str file-path ".g")                   ; Add .g extension
         output-file (or (:output-file options) (str file-path ".stp"))

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

;; Function to extract the model name from a filename pattern
(defn extract-model-name
  "Extract the model name from a filename pattern like 'washer_10_6_2'"
  [filename]
  (let [parts (str/split filename #"_")]
    (first parts)))

;; Ensure a file exists in the specified format
(defn ensure-format
  "Ensure a file exists in the specified format, converting if necessary"
  [file-name format]
  (let [;; Don't attempt any filename manipulation during this step
        ;; Use ls command to find the actual G file that matches our pattern
        _ (println "Looking for G file matching:" file-name)
        ls-result (sh "ls" "-la")
        _ (println "LS result:" (:out ls-result))

        ;; The .g file is created during model generation and should exist
        g-file (str file-name ".g")
        _ (println "Looking for G file:" g-file)

        ;; Extract just the model type (cylinder, washer, etc.) from the filename
        model-name (first (str/split (.getName (io/file file-name)) #"_"))
        _ (println "Model name extracted:" model-name)

        target-file (case format
                      :g g-file
                      :obj (str file-name ".obj")
                      :stl (str file-name ".stl")
                      :step (str file-name ".stp"))]
    (println "Ensuring format" format "for file" file-name "model name" model-name "target-file" target-file)

    ;; Check if the files actually exist in the current directory
    (println "Current directory:" (.getAbsolutePath (io/file ".")))
    (println "G file exists?:" (.exists (io/file g-file)))

    ;; Look for similar files
    (let [dir-files (into [] (.list (io/file ".")))
          g-files (filter #(.endsWith % ".g") dir-files)
          ;; Try to find a matching G file by pattern
          pattern-base (str/replace file-name #"\..*$" "")
          matching-g-file (first (filter #(.startsWith % pattern-base) g-files))]
      (println "Found G files in directory:" g-files)
      (println "Pattern we're looking for:" pattern-base)
      (println "Matching G file found:" matching-g-file)

      ;; If we found a matching G file, use that instead
      (let [actual-g-file (if (and matching-g-file (not (.exists (io/file g-file))))
                            matching-g-file
                            g-file)
            actual-base-name (str/replace actual-g-file #"\.g$" "")]
        (println "Using actual G file:" actual-g-file)

        (let [actual-target-file (case format
                                   :g actual-g-file
                                   :obj (str actual-base-name ".obj")
                                   :stl (str actual-base-name ".stl")
                                   :step (str actual-base-name ".stp"))]
          (println "Adjusted target file:" actual-target-file)
          (println "Adjusted target file:" actual-target-file)

          (if (.exists (io/file actual-target-file))
            {:status "success" :file actual-target-file}
            (case format
              :g (if (.exists (io/file actual-g-file))
                   {:status "success" :file actual-g-file}
                   {:status "error" :message (str "G file not found: " actual-g-file)})
              :obj (if (.exists (io/file actual-g-file))
                     (let [result (require 'cad-os.obj)
                           obj-fn (resolve 'cad-os.obj/convert-g-to-obj)]
                       (obj-fn actual-base-name [model-name] {:mesh true, :abs-tess-tol 0.01}))
                     {:status "error" :message (str "G file not found for OBJ conversion: " actual-g-file)})
              :stl (if (.exists (io/file actual-g-file))
                     (convert-g-to-stl actual-base-name [model-name] {:abs-tess-tol 0.01})
                     {:status "error" :message (str "G file not found for STL conversion: " actual-g-file)})
              :step (if (.exists (io/file actual-g-file))
                      (convert-g-to-step actual-base-name)
                      {:status "error" :message (str "G file not found for STEP conversion: " actual-g-file)}))))))))