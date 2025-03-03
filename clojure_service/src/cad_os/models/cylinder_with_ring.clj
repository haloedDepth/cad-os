(ns cad-os.models.cylinder-with-ring
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [cad-os.models.assembly :as assembly]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Schema definition for cylinder with ring
(def cylinder-with-ring-schema
  {:name "Cylinder with Ring"
   :description "A cylinder with a hollow cylinder (ring) positioned along its height"
   :parameters
   [{:name "cylinder-height"
     :type "number"
     :description "Height of the cylinder"
     :default 20.0}
    {:name "cylinder-diameter"
     :type "number"
     :description "Diameter of the cylinder"
     :default 10.0}
    {:name "ring-placement"
     :type "number"
     :description "Placement of the ring as percentage from bottom (0%) to top (100%)"
     :default 50.0}
    {:name "ring-thickness"
     :type "number"
     :description "Radial thickness of the ring (how thick the ring material is)"
     :default 2.0}
    {:name "ring-overhang"
     :type "number"
     :description "Gap between cylinder and inner edge of the ring"
     :default 2.0}
    {:name "ring-height"
     :type "number"
     :description "Height of the ring (vertical thickness)"
     :default 2.0}
    ;; Optional position parameters (hidden from UI by default)
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

   ;; Validation rules using expressions
   :validation-rules
   [{:expr "cylinder-height > 0.1"
     :message "Cylinder height must be greater than 0.1"}
    {:expr "cylinder-diameter > 0.1"
     :message "Cylinder diameter must be greater than 0.1"}
    {:expr "ring-placement >= 0 && ring-placement <= 100"
     :message "Ring placement must be between 0 and 100"}
    {:expr "ring-thickness > 0.1"
     :message "Ring thickness must be greater than 0.1"}
    {:expr "ring-overhang >= 0"
     :message "Ring overhang must be greater than or equal to 0"}
    {:expr "ring-height > 0.1"
     :message "Ring height must be greater than 0.1"}]})

;; Command generator function for cylinder with ring
(defn generate-cylinder-with-ring-commands
  "Generate commands to create a cylinder with a ring assembly"
  [params]
  (let [cylinder-height (get params :cylinder-height)
        cylinder-diameter (get params :cylinder-diameter)
        ring-placement (get params :ring-placement)
        ring-thickness (get params :ring-thickness)
        ring-overhang (get params :ring-overhang)
        ring-height (get params :ring-height)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)

        ;; Calculate actual ring position based on percentage
        ring-z-position (+ position-z (* (/ ring-placement 100) cylinder-height))

        ;; Create component specifications
        cylinder-spec {:type "cylinder"
                       :name "main-cylinder"
                       :params {:radius (/ cylinder-diameter 2)
                                :height cylinder-height
                                :position-x position-x
                                :position-y position-y
                                :position-z position-z}}

        ;; Create a hollow cylinder with updated parameters to match new schema
        ring-outer-diameter (+ cylinder-diameter (* 2 ring-overhang) (* 2 ring-thickness))
        ring-spec {:type "hollow-cylinder"
                   :name "ring"
                   :params {:diameter ring-outer-diameter     ;; Using new parameter name - outer diameter
                            :thickness ring-thickness         ;; Using new meaning - wall thickness
                            :height ring-height               ;; Using new parameter name - height instead of thickness
                            :position-x position-x
                            :position-y position-y
                            :position-z (- ring-z-position (/ ring-height 2))}}]

    ((:info log) "Generating cylinder with ring commands"
                 {:cylinder-height cylinder-height
                  :cylinder-diameter cylinder-diameter
                  :ring-placement ring-placement
                  :ring-z-position ring-z-position
                  :ring-radial-thickness ring-thickness
                  :ring-height ring-height
                  :ring-overhang ring-overhang
                  :ring-outer-diameter ring-outer-diameter})

    ;; Use the assembly function to create the complete assembly
    (assembly/create-assembly "cylinder-with-ring" [cylinder-spec ring-spec])))

;; Register the cylinder with ring model
(registry/register-model
 "cylinder-with-ring"
 cylinder-with-ring-schema
 generate-cylinder-with-ring-commands)