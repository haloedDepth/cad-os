(ns cad-os.core
  (:require
    [clojure.string :as str]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))

;; ------------------------------------------------------------------------------
;; 1. Example Data Structure for a Sphere
;; ------------------------------------------------------------------------------

(def example-sphere
  {:type   :sphere
   :name   "mySphere"
   :center [0 0 0]
   :radius 10.0})

;; ------------------------------------------------------------------------------
;; 2. Utility: Read the commands.edn
;; ------------------------------------------------------------------------------

(defn load-commands []
  (-> "commands.edn"
      io/resource
      slurp
      edn/read-string))
      
;; ------------------------------------------------------------------------------
;; 3. Utility: Simple template replacement
;;    Replaces placeholders like {name}, {x}, etc. in a string.
;; ------------------------------------------------------------------------------

(defn render-template
  "Given a template string like 'in {name} sph {x} {y} {z} {radius}'
   and a map {:name \"mySphere\", :x 0, :y 0, :z 0, :radius 10},
   returns 'in mySphere sph 0 0 0 10'"
  [template data]
  (reduce (fn [cmd [k v]]
            (str/replace cmd
                         (re-pattern (str "\\{" (name k) "\\}"))
                         (str v)))
          template
          data))

;; ------------------------------------------------------------------------------
;; 4. Transform sphere data -> MGED command string
;; ------------------------------------------------------------------------------

(defn sphere->mged-cmd
  "Given a sphere map (as above) and a sphere template,
   returns the properly substituted MGED command string."
  [sphere template]
  (let [{:keys [name center radius]} sphere
        [x y z] center]
    (render-template template
                     {:name   name
                      :x      x
                      :y      y
                      :z      z
                      :radius radius})))

