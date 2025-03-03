(ns cad-os.models.hollow-cylinder
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Schema definition for hollow cylinder
(def hollow-cylinder-schema
  {:name "Hollow Cylinder"
   :description "A simple hollow cylinder (ring) with inner and outer diameters"
   :parameters
   [{:name "outer-diameter"
     :type "number"
     :description "Outer diameter of the hollow cylinder"
     :default 10.0}
    {:name "inner-diameter"
     :type "number"
     :description "Inner diameter of the hollow cylinder"
     :default 6.0}
    {:name "thickness"
     :type "number"
     :description "Thickness of the hollow cylinder"
     :default 2.0}
    ;; Optional position parameters (hidden from UI by default)
    {:name "position-x"
     :type "number"
     :description "X position of the hollow cylinder"
     :default 0.0
     :hidden true}
    {:name "position-y"
     :type "number"
     :description "Y position of the hollow cylinder"
     :default 0.0
     :hidden true}
    {:name "position-z"
     :type "number"
     :description "Z position of the hollow cylinder"
     :default 0.0
     :hidden true}]

   ;; Validation rules using expressions
   :validation-rules
   [{:expr "outer-diameter > 0.1"
     :message "Outer diameter must be greater than 0.1"}
    {:expr "inner-diameter > 0.1"
     :message "Inner diameter must be greater than 0.1"}
    {:expr "thickness > 0.1"
     :message "Thickness must be greater than 0.1"}
    {:expr "inner-diameter < outer-diameter"
     :message "Inner diameter must be less than outer diameter"}]})

;; Command generator function for hollow cylinder
(defn generate-hollow-cylinder-commands
  "Generate commands to create a hollow cylinder model"
  [params]
  (let [outer-diameter (get params :outer-diameter)
        inner-diameter (get params :inner-diameter)
        thickness (get params :thickness)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)]

    ((:info log) "Generating hollow cylinder commands"
                 {:outer outer-diameter
                  :inner inner-diameter
                  :thickness thickness
                  :position [position-x position-y position-z]})

    [(commands/insert-right-circular-cylinder
      "outer" [position-x position-y position-z] [0 0 thickness] (/ outer-diameter 2))
     (commands/insert-right-circular-cylinder
      "inner" [position-x position-y position-z] [0 0 thickness] (/ inner-diameter 2))
     (commands/subtraction "hollow-cylinder" "outer" "inner")]))

;; Register the hollow cylinder model
(registry/register-model
 "hollow-cylinder"
 hollow-cylinder-schema
 generate-hollow-cylinder-commands)