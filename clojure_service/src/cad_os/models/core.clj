(ns cad-os.models.core
  (:require [cad-os.commands :as commands]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [cad-os.obj :as obj]
            [clojure.java.io :as io]))

(def mged-path "/usr/brlcad/rel-7.40.3/bin/mged")

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

(defn create-g-file
  "Create a .g file from a list of commands"
  [file-name commands]
  (let [joined (s/join ";" commands)]
    (println "Creating model:" file-name)
    (println "Running:" mged-path "-c" (str file-name ".g") joined)
    (let [shell-result (sh mged-path "-c" (str file-name ".g") joined)]
      (assoc shell-result :file (str file-name ".g")))))

(defn create-model
  "Create a model and convert it to OBJ format"
  [file-name model-name commands]
  (let [;; Create the .g file
        g-result (create-g-file file-name commands)
        g-file-path (str file-name ".g")

        ;; Wait for the .g file to exist
        g-file-exists (wait-for-file g-file-path 30 100)

        ;; Convert to OBJ if the .g file exists
        obj-result (if g-file-exists
                     (obj/convert-g-to-obj file-name [model-name]
                                           {:mesh true, :verbose true, :abs-tess-tol 0.01})
                     {:status "error" :message "G file was not created in time"})

        ;; Wait for the OBJ file to exist
        obj-file-path (str file-name ".obj")
        obj-file-exists (wait-for-file obj-file-path 30 100)]

    (if obj-file-exists
      {:status "success"
       :file-name file-name
       :g-result g-result
       :obj-result obj-result
       :obj-path (get obj-result :file (str file-name ".obj"))}
      {:status "error"
       :message "OBJ file was not created in time"
       :g-result g-result
       :obj-result obj-result})))