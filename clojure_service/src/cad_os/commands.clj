(ns cad-os.commands)

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

(defn validate-segments [vertex-list segment-list]
  (let [vertex-count (count vertex-list)
        invalid-segments (filter (fn [[start end]]
                                   (or (>= start vertex-count)
                                       (>= end vertex-count)
                                       (< start 0)
                                       (< end 0)))
                                 segment-list)]
    (if (empty? invalid-segments)
      true
      {:valid false
       :message (str "Error: " (count invalid-segments) " segments reference non-existent vertices")
       :invalid-segments invalid-segments})))

(defn format-segment [[start end]]
  (str "{line S " start " E " end "}"))

(defn insert-sketch [sketch-name v1 v2 v3 a1 a2 a3 b1 b2 b3 vertex-list segment-list]
  (let [validation-result (validate-segments vertex-list segment-list)]
    (if (= validation-result true)
      (str "put " sketch-name " sketch "
           "V {" v1 " " v2 " " v3 "} "
           "A {" a1 " " a2 " " a3 "} "
           "B {" b1 " " b2 " " b3 "} "
           "VL { " (clojure.string/join " " (map #(str "{" (first %) " " (second %) "}") vertex-list)) " } "
           "SL { " (clojure.string/join " " (map format-segment segment-list)) " }")
      (str "Sketch validation failed: " (:message validation-result)))))

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
