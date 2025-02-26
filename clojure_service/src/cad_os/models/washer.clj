(ns cad-os.models.washer
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; Schema definition for washer
(def washer-schema
  {:name "Washer"
   :description "A simple washer (ring) with inner and outer diameters"
   :parameters
   [{:name "outer-diameter"
     :type "number"
     :description "Outer diameter of the washer"
     :default 10.0}
    {:name "inner-diameter"
     :type "number"
     :description "Inner diameter of the washer"
     :default 6.0}
    {:name "thickness"
     :type "number"
     :description "Thickness of the washer"
     :default 2.0}]

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

;; Command generator function for washer
(defn generate-washer-commands
  "Generate commands to create a washer model"
  [params]
  (let [outer-diameter (get params :outer-diameter)
        inner-diameter (get params :inner-diameter)
        thickness (get params :thickness)]

    (println "Generating washer commands with: outer="
             outer-diameter "inner=" inner-diameter "thickness=" thickness)

    [(commands/insert-right-circular-cylinder
      "outer" 0 0 0 0 0 thickness (/ outer-diameter 2))
     (commands/insert-right-circular-cylinder
      "inner" 0 0 0 0 0 thickness (/ inner-diameter 2))
     (commands/subtraction "washer" "outer" "inner")]))

;; Register the washer model
(registry/register-model
 "washer"
 washer-schema
 generate-washer-commands)