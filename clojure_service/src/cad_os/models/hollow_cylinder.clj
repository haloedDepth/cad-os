(ns cad-os.models.hollow-cylinder
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Schema definition for hollow cylinder with new parameters
(def hollow-cylinder-schema
  {:name "Hollow Cylinder"
   :description "A simple hollow cylinder (ring) with diameter, height, and wall thickness"
   :parameters
   [{:name "diameter"
     :type "number"
     :description "Outer diameter of the hollow cylinder"
     :default 10.0}
    {:name "height"
     :type "number"
     :description "Height of the hollow cylinder"
     :default 2.0}
    {:name "thickness"
     :type "number"
     :description "Wall thickness (difference between outer and inner diameter)"
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

   ;; Updated validation rules
   :validation-rules
   [{:expr "diameter > 0.1"
     :message "Outer diameter must be greater than 0.1"}
    {:expr "height > 0.1"
     :message "Height must be greater than 0.1"}
    {:expr "thickness > 0.1"
     :message "Wall thickness must be greater than 0.1"}
    {:expr "thickness < (diameter / 2)"
     :message "Wall thickness must be less than radius (half of diameter)"}]})

;; Command generator function for hollow cylinder
(defn generate-hollow-cylinder-commands
  "Generate commands to create a hollow cylinder model with the new parametrization"
  [params]
  (let [diameter (get params :diameter)
        height (get params :height)
        thickness (get params :thickness)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)

        ;; Calculate inner diameter from diameter and thickness
        inner-diameter (- diameter (* 2 thickness))]

    ((:info log) "Generating hollow cylinder commands"
                 {:diameter diameter
                  :height height
                  :thickness thickness
                  :inner-diameter inner-diameter
                  :position [position-x position-y position-z]})

    [(commands/insert-right-circular-cylinder
      "outer" [position-x position-y position-z] [0 0 height] (/ diameter 2))
     (commands/insert-right-circular-cylinder
      "inner" [position-x position-y position-z] [0 0 height] (/ inner-diameter 2))
     (commands/subtraction "hollow-cylinder" "outer" "inner")]))

;; Register the hollow cylinder model
(registry/register-model
 "hollow-cylinder"
 hollow-cylinder-schema
 generate-hollow-cylinder-commands)