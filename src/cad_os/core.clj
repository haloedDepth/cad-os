(ns cad-os.core
  (:require [cad-os.commands :as commands]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]))

(def mged-path "/usr/brlcad/rel-7.40.3/bin/mged")  ;; Use the full path

(defn create-g [name f & args]
  (println "DEBUG: name =" name)
  (println "DEBUG: f =" f)
  (println "DEBUG: args =" args)
  (let [result (apply f args)
        joined (s/join ";" result)
        quoted (str "'" joined "'")]
    (println "DEBUG: Running:" mged-path "-c" (str name ".g") quoted)
    ;; Use -c flag to execute commands with the joined string in single quotes
    (sh mged-path "-c" (str name ".g") quoted)))


(defn washer
  [inner-diameter outer-diameter thickness]
  [(commands/insert-right-circular-cylinder "outer" 0 0 0 0 0 thickness (/ outer-diameter 2))
   (commands/insert-right-circular-cylinder "inner" 0 0 0 0 0 thickness (/ inner-diameter 2))
   (commands/subtraction "washer" "outer" "inner")])

(println "DEBUG: Testing washer function directly:")
(let [washer-result (washer 10 5 2)]
  (println "DEBUG: washer result =" washer-result))

(println "DEBUG: Now trying create-g:")
(create-g "washer" washer 10 5 2)

