(ns cad-os.models.vacuum-hose-adapter
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [cad-os.models.assembly :as assembly]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Schema definition for vacuum hose adapter
(def vacuum-hose-adapter-schema
  {:name "Vacuum Hose Adapter"
   :description "An adapter for connecting vacuum hoses with different diameters"
   :parameters
   [{:name "input-diameter"
     :type "number"
     :description "Diameter of the input connection"
     :default 30.0}
    {:name "output-diameter"
     :type "number"
     :description "Diameter of the output connection"
     :default 25.0}
    {:name "tapered-tube-length"
     :type "number"
     :description "Length of the transition section"
     :default 40.0}
    {:name "output-cylinder-length"
     :type "number"
     :description "Length of the output connection"
     :default 50.0}
    {:name "wall-thickness"
     :type "number"
     :description "Wall thickness throughout the adapter"
     :default 2.0}
    ;; Optional position parameters
    {:name "position-x"
     :type "number"
     :description "X position of the assembly"
     :default 0.0
     :hidden true}
    {:name "position-y"
     :type "number"
     :description "Y position of the assembly"
     :default 0.0
     :hidden true}
    {:name "position-z"
     :type "number"
     :description "Z position of the assembly"
     :default 0.0
     :hidden true}]

   ;; Validation rules
   :validation-rules
   [{:expr "input-diameter > 5.0"
     :message "Input diameter must be greater than 5mm"}
    {:expr "output-diameter > 5.0"
     :message "Output diameter must be greater than 5mm"}
    {:expr "tapered-tube-length > 5.0"
     :message "Tapered tube length must be greater than 5mm"}
    {:expr "output-cylinder-length > 5.0"
     :message "Output cylinder length must be greater than 5mm"}
    {:expr "wall-thickness > 0.5"
     :message "Wall thickness must be greater than 0.5mm"}
    {:expr "wall-thickness < (input-diameter / 3)"
     :message "Wall thickness must be less than 1/3 of input diameter"}
    {:expr "wall-thickness < (output-diameter / 3)"
     :message "Wall thickness must be less than 1/3 of output diameter"}]})

;; Command generator function for vacuum hose adapter
(defn generate-vacuum-hose-adapter-commands
  "Generate commands to create a vacuum hose adapter assembly"
  [params]
  (let [input-diameter (get params :input-diameter)
        output-diameter (get params :output-diameter)
        tapered-tube-length (get params :tapered-tube-length)
        output-cylinder-length (get params :output-cylinder-length)
        wall-thickness (get params :wall-thickness)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)

        ;; Fixed parameters
        input-cylinder-length 20.0  ;; Fixed length for input cylinder
        ring-height 5.0             ;; Fixed ring height
        ring-overhang 2.0           ;; Fixed ring overhang (how much it sticks out)

        ;; Calculate ring parameters
        ring-diameter (+ input-diameter (* 2 ring-overhang))
        ring-thickness ring-overhang ;; Make ring wall as thick as the overhang

        ;; Calculate positions for components
        input-cylinder-pos-z position-z
        ring-pos-z (+ input-cylinder-pos-z (- input-cylinder-length ring-height))
        tapered-tube-pos-z (+ input-cylinder-pos-z input-cylinder-length)
        output-cylinder-pos-z (+ tapered-tube-pos-z tapered-tube-length)

        ;; Create component specifications
        input-cylinder-spec
        {:type "hollow-cylinder"
         :name "input-section"
         :params {:diameter input-diameter
                  :height input-cylinder-length
                  :thickness wall-thickness
                  :position-x position-x
                  :position-y position-y
                  :position-z input-cylinder-pos-z}}

        ring-spec
        {:type "hollow-cylinder"
         :name "connection-ring"
         :params {:diameter ring-diameter
                  :height ring-height
                  :thickness ring-thickness
                  :position-x position-x
                  :position-y position-y
                  :position-z ring-pos-z}}

        tapered-tube-spec
        {:type "tapered-tube"
         :name "transition-section"
         :params {:bottom-diameter input-diameter
                  :top-diameter output-diameter
                  :height tapered-tube-length
                  :thickness wall-thickness
                  :position-x position-x
                  :position-y position-y
                  :position-z tapered-tube-pos-z}}

        output-cylinder-spec
        {:type "hollow-cylinder"
         :name "output-section"
         :params {:diameter output-diameter
                  :height output-cylinder-length
                  :thickness wall-thickness
                  :position-x position-x
                  :position-y position-y
                  :position-z output-cylinder-pos-z}}]

    ((:info log) "Generating vacuum hose adapter commands"
                 {:input-diameter input-diameter
                  :output-diameter output-diameter
                  :tapered-tube-length tapered-tube-length
                  :output-cylinder-length output-cylinder-length
                  :wall-thickness wall-thickness
                  :ring-diameter ring-diameter
                  :ring-thickness ring-thickness})

    ;; Use the assembly function to create the complete assembly
    (assembly/create-assembly
     "vacuum-hose-adapter"
     [input-cylinder-spec ring-spec tapered-tube-spec output-cylinder-spec])))

;; Register the vacuum hose adapter model
(registry/register-model
 "vacuum-hose-adapter"
 vacuum-hose-adapter-schema
 generate-vacuum-hose-adapter-commands)