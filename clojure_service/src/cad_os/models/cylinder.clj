(ns cad-os.models.cylinder
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; Schema definition for cylinder
(def cylinder-schema
  {:name "Cylinder"
   :description "A simple cylinder with specified radius and height"
   :parameters
   [{:name "radius"
     :type "number"
     :description "Radius of the cylinder"
     :default 5.0}
    {:name "height"
     :type "number"
     :description "Height of the cylinder"
     :default 10.0}
    ;; Optional position parameters (hidden from UI by default)
    {:name "position-x"
     :type "number"
     :description "X position of the cylinder"
     :default 0.0
     :hidden true}
    {:name "position-y"
     :type "number"
     :description "Y position of the cylinder"
     :default 0.0
     :hidden true}
    {:name "position-z"
     :type "number"
     :description "Z position of the cylinder"
     :default 0.0
     :hidden true}]

   ;; Validation rules using expressions
   :validation-rules
   [{:expr "radius > 0.1"
     :message "Radius must be greater than 0.1"}
    {:expr "height > 0.1"
     :message "Height must be greater than 0.1"}]})

;; Command generator function for cylinder
(defn generate-cylinder-commands
  "Generate commands to create a cylinder model"
  [params]
  (let [radius (get params :radius)
        height (get params :height)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)]

    (println "Generating cylinder commands with: radius=" radius "height=" height
             "position=(" position-x "," position-y "," position-z ")")

    [(commands/insert-right-circular-cylinder
      "cylinder"
      [position-x position-y position-z]
      [0 0 height]
      radius)]))

;; Register the cylinder model
(registry/register-model
 "cylinder"
 cylinder-schema
 generate-cylinder-commands)