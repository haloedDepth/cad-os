(ns cad-os.render
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

(defn render-model
  "Render a .g file to an image using the rt command-line tool.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to render
   - options: Map of options including:
     - :output-file (string) - Output image file path (-o)
     - :framebuffer (string) - Render to a framebuffer (-F)
     - :size (int) - Square image size (-s)
     - :width (int) - Image width (-w)
     - :height (int) - Image height (-n)
     - :background-color (string) - Background color as R/G/B (-C)
     - :white-background (boolean) - Set background to white (-W)
     - :disable-overlap (boolean) - Disable overlap reporting (-R)
     - :ambient-light (float) - Ambient light intensity (-A)
     - :azimuth (float) - Azimuth angle in degrees (-a)
     - :elevation (float) - Elevation angle in degrees (-e)
     - :perspective (float) - Perspective angle in degrees (-p)
     - :eye-distance (float) - Perspective eye distance (-E)
     - :hypersample (int) - Hypersample rays per pixel (-H)
     - :jitter (int) - Jitter pattern (-J)
     - :processors (int) - Number of processors to use (-P)
     - :tolerance (string) - Tolerance as distance or distance/angular (-T)
     - :incremental (boolean) - Enable incremental rendering (-i)
     - :top-to-bottom (boolean) - Render from top to bottom (-t)"
  ([file-path objects]
   (render-model file-path objects {}))

  ([file-path objects options]
   (let [file-path (str/replace file-path #"\.g$" "")  ; Remove .g extension if present
         g-file (str file-path ".g")                   ; Add .g extension
         default-output-file (str file-path ".png")    ; Default to PNG output
         output-file (or (:output-file options) default-output-file)

         ; Build command arguments
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

         ; Execute command
         _ (println "Executing:" (str/join " " (cons "rt" cmd-args)))
         result (apply sh "rt" cmd-args)]

     ; Return result
     (if (zero? (:exit result))
       (do
         (println "Command completed successfully, checking for output file:" output-file)
         (if (wait-for-file output-file 30 200)
           (do
             (println "Output file confirmed to exist:" output-file)
             {:status "success"
              :message (str "Created image: " output-file)
              :output (:out result)
              :file output-file})
           {:status "error"
            :message (str "Image file was not created despite successful command execution: " output-file)
            :output (:out result)
            :error "File not found after timeout"}))
       {:status "error"
        :message (str "Failed to render file: " g-file)
        :error (:err result)
        :exit-code (:exit result)}))))

;; ======== New Helper Functions for Orbit Views ========

(defn generate-orbit-view
  "Generate a single orbit view with specified azimuth and elevation angles.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to render
   - azimuth: Azimuth angle in degrees (horizontal rotation)
   - elevation: Elevation angle in degrees (vertical position)
   - output-file: Output image file path
   - options: Additional rendering options (like size, white-background, etc.)"
  [file-path objects azimuth elevation output-file & [extra-options]]
  (let [view-options (merge
                      {:azimuth azimuth
                       :elevation elevation
                       :output-file output-file
                       :white-background true
                       :size 800}
                      extra-options)]
    (render-model file-path objects view-options)))

(defn generate-standard-views
  "Generate the four standard views (front, right, back, left) for a model.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to render
   - output-dir: Directory to save the images
   - base-name: Base filename to use (will append _front, _right, etc.)
   - options: Additional rendering options"
  [file-path objects output-dir base-name & [options]]
  (let [standard-views [{:name "front" :azimuth 0 :elevation 30}
                        {:name "right" :azimuth 90 :elevation 30}
                        {:name "back" :azimuth 180 :elevation 30}
                        {:name "left" :azimuth 270 :elevation 30}]]
    (doseq [view standard-views]
      (let [output-file (format "%s/%s_%s.png"
                                output-dir
                                base-name
                                (:name view))]
        (generate-orbit-view
         file-path
         objects
         (:azimuth view)
         (:elevation view)
         output-file
         options)))))

(defn generate-orbit-views
  "Generate a complete set of orbit views around a model.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to render
   - output-dir: Directory to save the images
   - base-name: Base filename to use
   - options: Map of options including:
     - :azimuths - Vector of azimuth angles (default [0 45 90 135 180 225 270 315])
     - :elevations - Vector of elevation angles (default [0 30 60 90])
     - :render-options - Additional rendering options"
  [file-path objects output-dir base-name & [{:keys [azimuths elevations render-options]
                                              :or {azimuths [0 45 90 135 180 225 270 315]
                                                   elevations [0 30 60 90]
                                                   render-options {:size 800 :white-background true}}}]]
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
      (println (str "Rendering: azimuth=" azimuth ", elevation=" elevation))
      (generate-orbit-view
       file-path
       objects
       azimuth
       elevation
       output-file
       render-options))))

(defn generate-orbit-animation-frames
  "Generate a sequence of frames for animating orbit rotation.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to render
   - output-dir: Directory to save the images
   - base-name: Base filename to use
   - options: Map of options including:
     - :start-azimuth - Starting azimuth angle (default 0)
     - :end-azimuth - Ending azimuth angle (default 360)
     - :frame-count - Number of frames to generate (default 36)
     - :elevation - Elevation angle to use (default 30)
     - :render-options - Additional rendering options"
  [file-path objects output-dir base-name & [{:keys [start-azimuth end-azimuth frame-count elevation render-options]
                                              :or {start-azimuth 0
                                                   end-azimuth 360
                                                   frame-count 36
                                                   elevation 30
                                                   render-options {:size 800 :white-background true}}}]]
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
        (println (str "Rendering frame " frame " of " frame-count ": azimuth=" azimuth))
        (generate-orbit-view
         file-path
         objects
         azimuth
         elevation
         output-file
         render-options)))))