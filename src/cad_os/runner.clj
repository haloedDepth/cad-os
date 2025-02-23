(ns cad-os.runner
  (:require
    [clojure.java.shell :as shell]
    [clojure.java.io :as io]
    [cad-os.core :as core]))

(defn generate-mged-script
  "Given our sphere data, produce a sequence of MGED commands
   using templates from commands.edn, then return them as strings."
  []
  (let [commands-edn (core/load-commands)
        units-cmd    (:units commands-edn)
        sphere-cmd   (core/sphere->mged-cmd
                       core/example-sphere
                       (:sphere commands-edn))
        quit-cmd     (:quit commands-edn)]
    ;; Return a sequence of commands for demonstration
    [units-cmd
     sphere-cmd
     quit-cmd]))

(defn write-script-to-file
  "Writes each MGED command on its own line to the given file path."
  [commands filepath]
  (with-open [w (io/writer filepath)]
    (doseq [cmd commands]
      (.write w (str cmd "\n"))))
  filepath)

(defn run-mged!
  "Invokes MGED in non-interactive mode to create the final .g file.
   The 'proof-of-concept.g' is just a chosen name. Modify if you want."
  [script-file]
  (println "Running MGED script non-interactively...")
  (let [result (shell/sh "mged" "-c" "sphere.g" :in (slurp script-file))]
    (println "MGED output:" (:out result))
    (println "MGED errors:" (:err result))
    (println "MGED exit code:" (:exit result))
    (if (zero? (:exit result))
      (println "MGED ran successfully, sphere.g created.")
      (println "Something went wrong, check the error messages above."))))

(defn -main
  "Main entry point. Steps:
   1) Generate MGED commands
   2) Write them to a file
   3) Run MGED non-interactively
   4) Confirm that 'sphere.g' was created."
  []
  (println "Generating MGED commands from Clojure data...")
  (let [commands (generate-mged-script)
        script-file "mged-commands.txt"]
    (write-script-to-file commands script-file)
    (println "MGED commands saved to" script-file)
    (run-mged! script-file)))
