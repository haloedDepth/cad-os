(ns cad-os.commands
  (:require [clojure.string :as str]))

;; Ellipsoidis
(defn insert-ellipsoid
  [name
   center-x center-y center-z
   front-x front-y front-z
   right-x right-y right-z
   up-x up-y up-z]
  (str "in " name " ell " center-x " " center-y " " center-z " "
       front-x " " front-y " " front-z " "
       right-x " " right-y " " right-z " "
       up-x " " up-y " " up-z))

(defn insert-sphere
  [name center-x center-y center-z radius]
  (str "in " name " sph " center-x " " center-y " " center-z " " radius))

(defn insert-ellipsoid-g
  [name focus1-x focus1-y focus1-z
   focus2-x focus2-y focus2-z axis-length]
  (str "in " name " ellg " focus1-x " " focus1-y " " focus1-z " "
       focus2-x " " focus2-y " " focus2-z " " axis-length))

(defn insert-ellipsoid-1
  [name vertex-x vertex-y vertex-z
   vectorA-x vectorA-y vectorA-z radius-revolution]
  (str "in " name " ell1 " vertex-x " " vertex-y " " vertex-z " "
       vectorA-x " " vectorA-y " " vectorA-z " " radius-revolution))

(defn insert-elliptical-hyperboloid
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorA-x vectorA-y vectorA-z
   magnitudeB apex-to-asymptotes-distance]
  (str "in " name " ehy " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorA-x " " vectorA-y " " vectorA-z " "
       magnitudeB " " apex-to-asymptotes-distance))

(defn insert-elliptical-paraboloid
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorA-x vectorA-y vectorA-z
   magnitudeB]
  (str "in " name " epa " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorA-x " " vectorA-y " " vectorA-z " "
       magnitudeB))

;; Cones and cylinders
(defn insert-truncated-general-cone
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorA-x vectorA-y vectorA-z
   vectorB-x vectorB-y vectorB-z
   magnitudeC magnitudeD]
  (str "in " name " tgc " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorA-x " " vectorA-y " " vectorA-z " "
       vectorB-x " " vectorB-y " " vectorB-z " "
       magnitudeC " " magnitudeD))

(defn insert-right-circular-cylinder
  [name position-x position-y position-z
   vectorH-x vectorH-y vectorH-z
   radius]
  (str "in " name " rcc " position-x " " position-y " " position-z " "
       vectorH-x " " vectorH-y " " vectorH-z " " radius))

(defn insert-right-elliptical-cylinder
  [name position-x position-y position-z
   vectorH-x vectorH-y vectorH-z
   radius]
  (str "in " name " rec " position-x " " position-y " " position-z " "
       vectorH-x " " vectorH-y " " vectorH-z " " radius))

(defn insert-right-hyperbolic-cylinder
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorB-x vectorB-y vectorB-z
   rectangular-half-width apex-to-asymptotes-distance]
  (str "in " name " rhc " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorB-x " " vectorB-y " " vectorB-z " "
       rectangular-half-width " " apex-to-asymptotes-distance))

(defn insert-right-parabolic-cylinder
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorB-x vectorB-y vectorB-z
   rectangular-half-width]
  (str "in " name " rpc " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorB-x " " vectorB-y " " vectorB-z " "
       rectangular-half-width))

(defn insert-truncated-elliptical-cone
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   vectorA-x vectorA-y vectorA-z
   vectorB-x vectorB-y vectorB-z]
  (str "in " name " tec " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       vectorA-x " " vectorA-y " " vectorA-z " "
       vectorB-x " " vectorB-y " " vectorB-z))

(defn insert-truncated-right-circular-cone
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   radius-base radius-top]
  (str "in " name " trc " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       radius-base " " radius-top))

;; Other solids
(defn insert-torus
  [name center-x center-y center-z
   normal-x normal-y normal-z
   radius-revolution radius-tube]
  (str "in " name " tor " center-x " " center-y " " center-z " "
       normal-x " " normal-y " " normal-z " "
       radius-revolution " " radius-tube))

(defn insert-elliptical-torus
  [name center-x center-y center-z
   normal-x normal-y normal-z
   radius-revolution vectorC-x vectorC-y vectorC-z
   magnitude-semi-minor-axis]
  (str "in " name " eto " center-x " " center-y " " center-z " "
       normal-x " " normal-y " " normal-z " "
       radius-revolution " " vectorC-x " " vectorC-y " " vectorC-z " "
       magnitude-semi-minor-axis))

(defn insert-conical-particle
  [name vertex-x vertex-y vertex-z
   vectorH-x vectorH-y vectorH-z
   radius-v radius-h]
  (str "in " name " part " vertex-x " " vertex-y " " vertex-z " "
       vectorH-x " " vectorH-y " " vectorH-z " "
       radius-v " " radius-h))

;; Sketch segment formatting and validation
(defn format-line-segment [[start end]]
  (str "{line S " start " E " end "}"))

(defn validate-arc-params
  "Validates arc parameters based on BRL-CAD rules"
  [[start end radius left-right orientation :as arc-params]]
  (let [validation-errors
        (cond-> []
          (and (= start end) (>= radius 0))
          (conj "Start and end points must be different for regular arcs")

          (and (< radius 0) (not= start end))
          (conj "Full circles (negative radius) must have same start and end point")

          (not (contains? #{0 1} left-right))
          (conj "Left-right parameter must be 0 or 1")

          (not (contains? #{0 1} orientation))
          (conj "Orientation parameter must be 0 or 1"))]
    {:valid (empty? validation-errors)
     :errors validation-errors}))

(defn format-arc-segment [[start end radius left-right orientation :as arc-params]]
  (let [validation (validate-arc-params arc-params)]
    (if (:valid validation)
      (str "{carc S " start " E " end " R " radius " L " left-right " O " orientation "}")
      (throw (Exception. (str "Invalid arc parameters: " (str/join ", " (:errors validation))))))))

(defn format-bezier-segment [[degree & points]]
  (if (and (number? degree)
           (>= degree 1)
           (<= degree (count points)))
    (str "{bezier D " degree " P {" (str/join " " points) "}}")
    (throw (Exception. "Invalid bezier parameters: degree must be between 1 and point count"))))

(defn format-segment [segment]
  (case (first segment)
    :line (format-line-segment (rest segment))
    :arc (format-arc-segment (rest segment))
    :bezier (format-bezier-segment (rest segment))
    (throw (Exception. (str "Unknown segment type: " (first segment))))))

(defn validate-segments [vertex-list segments]
  (let [vertex-count (count vertex-list)
        validate-point (fn [p] (and (>= p 0) (< p vertex-count)))
        validate-points (fn [points] (every? validate-point points))]
    (reduce (fn [result segment]
              (let [segment-type (first segment)
                    points (case segment-type
                             :line (rest segment)
                             :arc [(second segment) (nth segment 2)]
                             :bezier (drop 2 segment)
                             [])]
                (cond
                  (not (contains? #{:line :arc :bezier} segment-type))
                  (update result :invalid-segments conj
                          {:segment segment
                           :error "Invalid segment type"})

                  (not (validate-points points))
                  (update result :invalid-segments conj
                          {:segment segment
                           :error "Points out of range"})

                  (= segment-type :arc)
                  (let [arc-validation (validate-arc-params (rest segment))]
                    (if (:valid arc-validation)
                      result
                      (update result :invalid-segments conj
                              {:segment segment
                               :error (:errors arc-validation)})))

                  (= segment-type :bezier)
                  (let [degree (second segment)
                        point-count (- (count segment) 2)]
                    (if (and (number? degree)
                             (>= degree 1)
                             (<= degree point-count))
                      result
                      (update result :invalid-segments conj
                              {:segment segment
                               :error "Invalid bezier degree"})))

                  :else result)))
            {:valid true :invalid-segments []}
            segments)))

(defn insert-sketch [sketch-name v1 v2 v3 a1 a2 a3 b1 b2 b3 vertex-list segments]
  (let [validation-result (validate-segments vertex-list segments)]
    (if (:valid validation-result)
      (str "put " sketch-name " sketch "
           "V {" v1 " " v2 " " v3 "} "
           "A {" a1 " " a2 " " a3 "} "
           "B {" b1 " " b2 " " b3 "} "
           "VL { " (str/join " " (map #(str "{" (first %) " " (second %) "}") vertex-list)) " } "
           "SL { " (str/join " " (map format-segment segments)) " }")
      (str "Sketch validation failed: "
           (str/join ", "
                     (map #(str (:error %) " in " (:segment %))
                          (:invalid-segments validation-result)))))))

(defn insert-sketch-revolve
  [name vertex-x vertex-y vertex-z
   axis-x axis-y axis-z
   start-x start-y start-z
   angle
   sketch-name]
  (str "in " name " revolve "
       vertex-x " " vertex-y " " vertex-z " "
       axis-x " " axis-y " " axis-z " "
       start-x " " start-y " " start-z " "
       angle " " sketch-name))

(defn insert-sketch-extrude
  [name vertex-x vertex-y vertex-z
   Hx Hy Hz
   Ax Ay Az
   Bx By Bz
   sketch-name]
  (str "in " name " extrude "
       vertex-x " " vertex-y " " vertex-z " "
       Hx " " Hy " " Hz " "
       Ax " " Ay " " Az " "
       Bx " " By " " Bz " " sketch-name))

;; Boolean
(defn union [name shape1 shape2]
  (str "r " name " u " shape1 " u " shape2))

(defn subtraction [name shape1 shape2]
  (str "r " name " u " shape1 " - " shape2))

(defn intersection [name shape1 shape2]
  (str "r " name " u " shape1 " + " shape2))

;; Other

(defn copy-object
  [from-object to-object]
  (str "cp " from-object " " to-object))

;; Arbitrary polyhedra
(defn insert-arb4
  [name 
   v1x v1y v1z 
   v2x v2y v2z 
   v3x v3y v3z 
   v4x v4y v4z]
  (str "in " name " arb4 "
       v1x " " v1y " " v1z " "
       v2x " " v2y " " v2z " "
       v3x " " v3y " " v3z " "
       v4x " " v4y " " v4z))

(defn insert-arb5
  [name 
   v1x v1y v1z 
   v2x v2y v2z 
   v3x v3y v3z 
   v4x v4y v4z 
   v5x v5y v5z]
  (str "in " name " arb5 "
       v1x " " v1y " " v1z " "
       v2x " " v2y " " v2z " "
       v3x " " v3y " " v3z " "
       v4x " " v4y " " v4z " "
       v5x " " v5y " " v5z))

(defn insert-arb6
  [name 
   v1x v1y v1z 
   v2x v2y v2z 
   v3x v3y v3z 
   v4x v4y v4z 
   v5x v5y v5z 
   v6x v6y v6z]
  (str "in " name " arb6 "
       v1x " " v1y " " v1z " "
       v2x " " v2y " " v2z " "
       v3x " " v3y " " v3z " "
       v4x " " v4y " " v4z " "
       v5x " " v5y " " v5z " "
       v6x " " v6y " " v6z))

(defn insert-arb7
  [name 
   v1x v1y v1z 
   v2x v2y v2z 
   v3x v3y v3z 
   v4x v4y v4z 
   v5x v5y v5z 
   v6x v6y v6z 
   v7x v7y v7z]
  (str "in " name " arb7 "
       v1x " " v1y " " v1z " "
       v2x " " v2y " " v2z " "
       v3x " " v3y " " v3z " "
       v4x " " v4y " " v4z " "
       v5x " " v5y " " v5z " "
       v6x " " v6y " " v6z " "
       v7x " " v7y " " v7z))

(defn insert-arb8
  [name 
   v1x v1y v1z 
   v2x v2y v2z 
   v3x v3y v3z 
   v4x v4y v4z 
   v5x v5y v5z 
   v6x v6y v6z 
   v7x v7y v7z 
   v8x v8y v8z]
  (str "in " name " arb8 "
       v1x " " v1y " " v1z " "
       v2x " " v2y " " v2z " "
       v3x " " v3y " " v3z " "
       v4x " " v4y " " v4z " "
       v5x " " v5y " " v5z " "
       v6x " " v6y " " v6z " "
       v7x " " v7y " " v7z " "
       v8x " " v8y " " v8z))