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
     :default 10.0}]

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
        height (get params :height)]

    (println "Generating cylinder commands with: radius=" radius "height=" height)

    [(commands/insert-right-circular-cylinder "cylinder" 0 0 0 0 0 height radius)]))

;; Register the cylinder model
(registry/register-model
 "cylinder"
 cylinder-schema
 generate-cylinder-commands)