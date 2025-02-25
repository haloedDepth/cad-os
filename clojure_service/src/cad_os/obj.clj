(ns cad-os.obj
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn convert-g-to-obj
  "Convert a .g file to .obj format using the g-obj command-line tool.
   
   Parameters:
   - file-path: Path to the .g file without extension
   - objects: Collection of object names within the .g file to convert
   - options: Map of options including:
     - :verbose (boolean) - Enable verbose mode (-v)
     - :mesh (boolean) - Generate mesh objects (-m)
     - :invert (boolean) - Invert normals (-i)
     - :units (boolean) - Write units info (-u)
     - :no-colors (boolean) - Don't convert colors (-X followed by lvl)
     - :colors (boolean) - Convert colors (-x followed by lvl)
     - :abs-tess-tol (float) - Absolute tessellation tolerance (-a)
     - :rel-tess-tol (float) - Relative tessellation tolerance (-r)
     - :norm-tess-tol (float) - Normal tessellation tolerance (-n)
     - :parallelism (int) - Number of CPUs to use (-P)
     - :error-file (string) - Error file name (-e)
     - :dist-calc-tol (float) - Distance calculation tolerance (-D)
     - :output-file (string) - Output file name (-o)"
  ([file-path objects]
   (convert-g-to-obj file-path objects {}))
  
  ([file-path objects options]
   (let [file-path (str/replace file-path #"\.g$" "")  ; Remove .g extension if present
         g-file (str file-path ".g")                   ; Add .g extension
         output-file (or (:output-file options) (str file-path ".obj"))
         
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
         _ (println "Executing:" (str/join " " (cons "g-obj" cmd-args)))
         result (apply sh "g-obj" cmd-args)]
     
     ; Return result
     (if (zero? (:exit result))
       {:status "success"
        :message (str "Created OBJ file: " output-file)
        :output (:out result)
        :file output-file}
       {:status "error"
        :message (str "Failed to convert file: " g-file)
        :error (:err result)
        :exit-code (:exit result)}))))

;; Example usage:
;; (convert-g-to-obj "path/to/model" ["object1" "object2"])
;; 
;; With options:
;; (convert-g-to-obj "path/to/model" ["object1" "object2"] 
;;                   {:mesh true 
;;                    :verbose true
;;                    :abs-tess-tol 0.01
;;                    :output-file "custom-output.obj"})