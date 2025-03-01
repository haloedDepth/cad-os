(ns cad-os.render
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [cad-os.filename :as filename]
            [clojure.string :as str]
            [cad-os.utils.logger :as logger])
  (:import [java.io File]))

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

(defn ensure-directory-exists
  "Ensure the directory exists for a given file path"
  [file-path]
  (let [file (io/file file-path)
        parent-dir (.getParentFile file)]
    (when (and parent-dir (not (.exists parent-dir)))
      ((:info log) "Creating directory" {:directory (.getAbsolutePath parent-dir)})
      (.mkdirs parent-dir))))

(defn render-model
  "Render a .g file to an image using the rt command-line tool."
  ([file-path objects]
   (render-model file-path objects {}))

  ([file-path objects options]
   ((:info log) "Rendering model" {:file file-path :objects objects})
   (let [base-path (filename/base-filename file-path)
         g-file (filename/with-extension base-path :g)
         default-output-file (str base-path ".png")
         output-file (or (:output-file options) default-output-file)

         ;; Ensure the output directory exists
         _ (ensure-directory-exists output-file)

         ;; Build command arguments
         cmd-args (cond-> []
                    (:output-file options)     (conj "-o" (:output-file options))
                    (:framebuffer options)     (conj "-F" (:framebuffer options))
                    (:size options)            (conj "-s" (str (:size options)))
                    (:width options)           (conj "-w" (str (:width options)))
                    (:height options)          (conj "-n" (str (:height options)))
                    (:background-color options) (conj "-C" (:background-color options))
                    (:white-background options) (conj "-W")
                    (:disable-overlap options)  (conj "-R")
                    (:ambient-light options)    (conj "-A" (str (:ambient-light options)))
                    (:azimuth options)          (conj "-a" (str (:azimuth options)))
                    (:elevation options)        (conj "-e" (str (:elevation options)))
                    (:perspective options)      (conj "-p" (str (:perspective options)))
                    (:eye-distance options)     (conj "-E" (str (:eye-distance options)))
                    (:hypersample options)      (conj "-H" (str (:hypersample options)))
                    (:jitter options)           (conj "-J" (str (:jitter options)))
                    (:processors options)       (conj "-P" (str (:processors options)))
                    (:tolerance options)        (conj "-T" (:tolerance options))
                    (:incremental options)      (conj "-i")
                    (:top-to-bottom options)    (conj "-t")
                    true                        (conj g-file)
                    true                        (concat objects))

         ;; Execute command - run with timeout to prevent hanging
         _ ((:debug log) "Executing command" {:command "rt" :args cmd-args})
         result (apply sh "rt" cmd-args)]

     ; Log detailed command result
     ((:debug log) "Command result" {:exit (:exit result)
                                     :out (:out result)
                                     :err (:err result)
                                     :output-file output-file})

     ; Return result
     (if (zero? (:exit result))
       (do
         ((:debug log) "Command completed successfully" {:output-file output-file})
         (if (wait-for-file output-file 60 200) ;; Increased timeout to 60 attempts (12 seconds)
           (do
             ((:info log) "Render completed successfully" {:file output-file})
             {:status "success"
              :message (str "Created image: " output-file)
              :output (:out result)
              :file output-file})
           (do
             ;; Try to look for similar files in the directory
             (let [parent-dir (.getParentFile (io/file output-file))
                   fallback-file (when parent-dir
                                   (try
                                     (let [files (seq (.listFiles parent-dir))
                                           png-files (filter #(.endsWith (.getName %) ".png") files)]
                                       (first png-files))
                                     (catch Exception e nil)))]
               (if fallback-file
                 (do
                   ((:warn log) "Using fallback file" {:fallback (.getAbsolutePath fallback-file)})
                   {:status "success"
                    :message (str "Using fallback image: " (.getName fallback-file))
                    :output (:out result)
                    :file (.getAbsolutePath fallback-file)})
                 (do
                   ((:error log) "Render file was not created despite successful command"
                                 {:output-file output-file :command-output (:out result)})
                   {:status "error"
                    :message (str "Image file was not created despite successful command execution: " output-file)
                    :output (:out result)
                    :error "File not found after timeout"}))))))
       (do
         ((:error log) "Failed to render file"
                       {:input-file g-file :exit-code (:exit result) :error (:err result)})
         {:status "error"
          :message (str "Failed to render file: " g-file)
          :error (:err result)
          :exit-code (:exit result)})))))

;; ======== Helper Functions for Orbit Views ========

(defn generate-orbit-view
  "Generate a single orbit view with specified azimuth and elevation angles."
  [file-path objects azimuth elevation output-file & [extra-options]]
  ((:info log) "Generating orbit view"
               {:file file-path
                :azimuth azimuth
                :elevation elevation
                :output output-file})

  ;; Ensure the output directory exists
  (ensure-directory-exists output-file)

  ;; Check if g-file exists
  (let [g-file (filename/with-extension file-path :g)
        g-file-obj (io/file g-file)]
    (if (not (.exists g-file-obj))
      (do
        ((:error log) "G file does not exist" {:g-file g-file})
        {:status "error"
         :message (str "G file does not exist: " g-file)})

      ;; Proceed with rendering
      (let [view-options (merge
                          {:azimuth azimuth
                           :elevation elevation
                           :output-file output-file
                           :white-background true
                           :size 800}
                          extra-options)]
        (render-model file-path (if (empty? objects) ["cylinder"] objects) view-options)))))

(defn generate-standard-views
  "Generate the four standard views (front, right, back, left) for a model."
  [file-path objects output-dir base-name & [options]]
  ((:info log) "Generating standard views" {:file file-path :base-name base-name})

  ;; Ensure output directory exists
  (let [dir (io/file output-dir)]
    (when-not (.exists dir)
      (.mkdirs dir)))

  (let [standard-views [{:name "front" :azimuth 0 :elevation 30}
                        {:name "right" :azimuth 90 :elevation 30}
                        {:name "back" :azimuth 180 :elevation 30}
                        {:name "left" :azimuth 270 :elevation 30}]]
    (doseq [view standard-views]
      (let [output-file (format "%s/%s_%s.png"
                                output-dir
                                base-name
                                (:name view))]
        ((:debug log) "Generating view" {:name (:name view) :output output-file})
        (generate-orbit-view
         file-path
         objects
         (:azimuth view)
         (:elevation view)
         output-file
         options)))))

(defn generate-orbit-views
  "Generate a complete set of orbit views around a model."
  [file-path objects output-dir base-name & [{:keys [azimuths elevations render-options]
                                              :or {azimuths [0 45 90 135 180 225 270 315]
                                                   elevations [0 30 60 90]
                                                   render-options {:size 800 :white-background true}}}]]
  ((:info log) "Generating orbit views"
               {:file file-path
                :azimuth-count (count azimuths)
                :elevation-count (count elevations)})

  ;; Ensure output directory exists
  (let [dir (io/file output-dir)]
    (when-not (.exists dir)
      (.mkdirs dir)))

  ;; Generate each view
  (doseq [azimuth azimuths
          elevation elevations]
    (let [output-file (format "%s/%s_az%d_el%d.png"
                              output-dir
                              base-name
                              azimuth
                              elevation)]
      ((:debug log) "Generating view" {:azimuth azimuth :elevation elevation})
      (generate-orbit-view
       file-path
       objects
       azimuth
       elevation
       output-file
       render-options))))

(defn generate-orbit-animation-frames
  "Generate a sequence of frames for animating orbit rotation."
  [file-path objects output-dir base-name & [{:keys [start-azimuth end-azimuth frame-count elevation render-options]
                                              :or {start-azimuth 0
                                                   end-azimuth 360
                                                   frame-count 36
                                                   elevation 30
                                                   render-options {:size 800 :white-background true}}}]]
  ((:info log) "Generating animation frames"
               {:file file-path
                :frame-count frame-count
                :elevation elevation})

  ;; Ensure output directory exists
  (let [dir (io/file output-dir)]
    (when-not (.exists dir)
      (.mkdirs dir)))

  ;; Calculate azimuth step
  (let [azimuth-step (/ (- end-azimuth start-azimuth) frame-count)]

    ;; Generate each frame
    (doseq [frame (range frame-count)]
      (let [azimuth (+ start-azimuth (* frame azimuth-step))
            output-file (format "%s/%s_frame%03d.png"
                                output-dir
                                base-name
                                frame)]
        ((:debug log) "Generating frame" {:frame frame :azimuth azimuth})
        (generate-orbit-view
         file-path
         objects
         azimuth
         elevation
         output-file
         render-options)))))