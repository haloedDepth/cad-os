(ns cad-os.models.tapered-tube
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Schema definition for tapered tube (hollow cone)
(def tapered-tube-schema
  {:name "Tapered Tube"
   :description "A hollow conical tube that tapers from a larger diameter to a smaller diameter"
   :parameters
   [{:name "top-diameter"
     :type "number"
     :description "Diameter at the top (smaller end) of the tube"
     :default 5.0}
    {:name "bottom-diameter"
     :type "number"
     :description "Diameter at the bottom (larger end) of the tube"
     :default 10.0}
    {:name "height"
     :type "number"
     :description "Height (length) of the tube"
     :default 15.0}
    {:name "thickness"
     :type "number"
     :description "Wall thickness of the tube"
     :default 1.0}
    ;; Optional position parameters (hidden from UI by default)
    {:name "position-x"
     :type "number"
     :description "X position of the tapered tube"
     :default 0.0
     :hidden true}
    {:name "position-y"
     :type "number"
     :description "Y position of the tapered tube"
     :default 0.0
     :hidden true}
    {:name "position-z"
     :type "number"
     :description "Z position of the tapered tube (at base)"
     :default 0.0
     :hidden true}]

   ;; Validation rules using expressions
   :validation-rules
   [{:expr "top-diameter > 0.1"
     :message "Top diameter must be greater than 0.1"}
    {:expr "bottom-diameter > 0.1"
     :message "Bottom diameter must be greater than 0.1"}
    {:expr "height > 0.1"
     :message "Height must be greater than 0.1"}
    {:expr "thickness > 0.1"
     :message "Wall thickness must be greater than 0.1"}
    {:expr "thickness < (top-diameter / 2)"
     :message "Wall thickness must be less than half of the top diameter"}
    {:expr "thickness < (bottom-diameter / 2)"
     :message "Wall thickness must be less than half of the bottom diameter"}]})

;; Command generator function for tapered tube
(defn generate-tapered-tube-commands
  "Generate commands to create a tapered tube (hollow cone) model"
  [params]
  (let [top-diameter (get params :top-diameter)
        bottom-diameter (get params :bottom-diameter)
        height (get params :height)
        thickness (get params :thickness)
        position-x (get params :position-x 0)
        position-y (get params :position-y 0)
        position-z (get params :position-z 0)

        ;; Calculate inner diameters by subtracting twice the thickness
        inner-top-diameter (- top-diameter (* 2 thickness))
        inner-bottom-diameter (- bottom-diameter (* 2 thickness))

        ;; Ensure inner diameters don't go negative
        inner-top-diameter (max 0.1 inner-top-diameter)
        inner-bottom-diameter (max 0.1 inner-bottom-diameter)]

    ((:info log) "Generating tapered tube commands"
                 {:top-diameter top-diameter
                  :bottom-diameter bottom-diameter
                  :height height
                  :thickness thickness
                  :inner-top-diameter inner-top-diameter
                  :inner-bottom-diameter inner-bottom-diameter
                  :position [position-x position-y position-z]})

    ;; Create outer truncated cone
    [(commands/insert-truncated-right-circular-cone
      "outer-cone"
      [position-x position-y position-z]  ;; base position
      [0 0 height]                        ;; height vector
      (/ bottom-diameter 2)               ;; base radius
      (/ top-diameter 2))                 ;; top radius

     ;; Create inner truncated cone - slightly shorter to avoid coplanar faces
     (commands/insert-truncated-right-circular-cone
      "inner-cone"
      [position-x position-y position-z]  ;; base position
      [0 0 height]                        ;; height vector
      (/ inner-bottom-diameter 2)         ;; base radius
      (/ inner-top-diameter 2))           ;; top radius

     ;; Subtract inner cone from outer cone
     (commands/subtraction "tapered-tube" "outer-cone" "inner-cone")]))

;; Register the tapered tube model
(registry/register-model
 "tapered-tube"
 tapered-tube-schema
 generate-tapered-tube-commands)