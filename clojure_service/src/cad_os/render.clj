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