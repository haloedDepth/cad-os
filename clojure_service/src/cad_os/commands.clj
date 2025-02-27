(ns cad-os.commands
  (:require [clojure.string :as str]))

;; Helper functions
(defn vec->str
  "Convert a vector of numbers to space-separated string"
  [v]
  (str/join " " v))

(defn point->str
  "Convert a 3D point vector to space-separated string"
  [[x y z]]
  (str x " " y " " z))

(defn validate-point
  "Validate that v is a 3D point vector"
  [v]
  (when-not (and (vector? v) (= (count v) 3) (every? number? v))
    (throw (Exception. "Expected a vector of 3 numbers [x y z]"))))

(defn validate-points
  "Validate multiple 3D point vectors"
  [points]
  (doseq [p points]
    (validate-point p)))

;; Ellipsoids
(defn insert-ellipsoid
  "Create ellipsoid using center and three vectors"
  [name center front right up]
  (validate-points [center front right up])
  (str "in " name " ell "
       (point->str center) " "
       (point->str front) " "
       (point->str right) " "
       (point->str up)))

(defn insert-sphere
  "Create sphere using center point and radius"
  [name center radius]
  (validate-point center)
  (str "in " name " sph " (point->str center) " " radius))

(defn insert-ellipsoid-g
  "Create ellipsoid using two foci and axis length"
  [name focus1 focus2 axis-length]
  (validate-points [focus1 focus2])
  (str "in " name " ellg "
       (point->str focus1) " "
       (point->str focus2) " "
       axis-length))

(defn insert-ellipsoid-1
  "Create ellipsoid using vertex, vector, and revolution radius"
  [name vertex vector-a radius-revolution]
  (validate-points [vertex vector-a])
  (str "in " name " ell1 "
       (point->str vertex) " "
       (point->str vector-a) " "
       radius-revolution))

(defn insert-elliptical-hyperboloid
  "Create elliptical hyperboloid"
  [name vertex vector-h vector-a magnitude-b apex-distance]
  (validate-points [vertex vector-h vector-a])
  (str "in " name " ehy "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-a) " "
       magnitude-b " " apex-distance))

(defn insert-elliptical-paraboloid
  "Create elliptical paraboloid"
  [name vertex vector-h vector-a magnitude-b]
  (validate-points [vertex vector-h vector-a])
  (str "in " name " epa "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-a) " "
       magnitude-b))

;; Cones and cylinders
(defn insert-truncated-general-cone
  "Create truncated general cone"
  [name vertex vector-h vector-a vector-b magnitude-c magnitude-d]
  (validate-points [vertex vector-h vector-a vector-b])
  (str "in " name " tgc "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-a) " "
       (point->str vector-b) " "
       magnitude-c " " magnitude-d))

(defn insert-right-circular-cylinder
  "Create right circular cylinder"
  [name position vector-h radius]
  (validate-points [position vector-h])
  (str "in " name " rcc "
       (point->str position) " "
       (point->str vector-h) " "
       radius))

(defn insert-right-elliptical-cylinder
  "Create right elliptical cylinder"
  [name position vector-h radius]
  (validate-points [position vector-h])
  (str "in " name " rec "
       (point->str position) " "
       (point->str vector-h) " "
       radius))

(defn insert-right-hyperbolic-cylinder
  "Create right hyperbolic cylinder"
  [name vertex vector-h vector-b rectangular-half-width apex-distance]
  (validate-points [vertex vector-h vector-b])
  (str "in " name " rhc "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-b) " "
       rectangular-half-width " " apex-distance))

(defn insert-right-parabolic-cylinder
  "Create right parabolic cylinder"
  [name vertex vector-h vector-b rectangular-half-width]
  (validate-points [vertex vector-h vector-b])
  (str "in " name " rpc "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-b) " "
       rectangular-half-width))

(defn insert-truncated-elliptical-cone
  "Create truncated elliptical cone"
  [name vertex vector-h vector-a vector-b]
  (validate-points [vertex vector-h vector-a vector-b])
  (str "in " name " tec "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-a) " "
       (point->str vector-b)))

(defn insert-truncated-right-circular-cone
  "Create truncated right circular cone"
  [name vertex vector-h radius-base radius-top]
  (validate-points [vertex vector-h])
  (str "in " name " trc "
       (point->str vertex) " "
       (point->str vector-h) " "
       radius-base " " radius-top))

;; Other solids
(defn insert-torus
  "Create torus"
  [name center normal radius-revolution radius-tube]
  (validate-points [center normal])
  (str "in " name " tor "
       (point->str center) " "
       (point->str normal) " "
       radius-revolution " " radius-tube))

(defn insert-elliptical-torus
  "Create elliptical torus"
  [name center normal radius-revolution vector-c magnitude-semi-minor-axis]
  (validate-points [center normal vector-c])
  (str "in " name " eto "
       (point->str center) " "
       (point->str normal) " "
       radius-revolution " "
       (point->str vector-c) " "
       magnitude-semi-minor-axis))

(defn insert-conical-particle
  "Create conical particle"
  [name vertex vector-h radius-v radius-h]
  (validate-points [vertex vector-h])
  (str "in " name " part "
       (point->str vertex) " "
       (point->str vector-h) " "
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

(defn insert-sketch
  "Create a sketch"
  [sketch-name v-point a-point b-point vertex-list segments]
  (validate-points [v-point a-point b-point])
  (let [validation-result (validate-segments vertex-list segments)]
    (if (:valid validation-result)
      (str "put " sketch-name " sketch "
           "V {" (point->str v-point) "} "
           "A {" (point->str a-point) "} "
           "B {" (point->str b-point) "} "
           "VL { " (str/join " " (map #(str "{" (first %) " " (second %) "}") vertex-list)) " } "
           "SL { " (str/join " " (map format-segment segments)) " }")
      (str "Sketch validation failed: "
           (str/join ", "
                     (map #(str (:error %) " in " (:segment %))
                          (:invalid-segments validation-result)))))))

(defn insert-sketch-revolve
  "Create a sketch revolve"
  [name vertex axis start angle sketch-name]
  (validate-points [vertex axis start])
  (str "in " name " revolve "
       (point->str vertex) " "
       (point->str axis) " "
       (point->str start) " "
       angle " " sketch-name))

(defn insert-sketch-extrude
  "Create a sketch extrude"
  [name vertex vector-h vector-a vector-b sketch-name]
  (validate-points [vertex vector-h vector-a vector-b])
  (str "in " name " extrude "
       (point->str vertex) " "
       (point->str vector-h) " "
       (point->str vector-a) " "
       (point->str vector-b) " "
       sketch-name))

;; Boolean operations
(defn union [name shape1 shape2]
  (str "r " name " u " shape1 " u " shape2))

(defn subtraction [name shape1 shape2]
  (str "r " name " u " shape1 " - " shape2))

(defn intersection [name shape1 shape2]
  (str "r " name " u " shape1 " + " shape2))

;; Other operations
(defn copy-object [from-object to-object]
  (str "cp " from-object " " to-object))

;; Arbitrary polyhedra
(defn validate-vertices [vertices expected-count]
  (when-not (= (count vertices) expected-count)
    (throw (Exception. (str "Expected " expected-count " vertices, got " (count vertices)))))
  (validate-points vertices))

(defn vertices->string [vertices]
  (str/join " " (mapcat point->str vertices)))

(defn insert-arb4
  "Create an ARB4 primitive with 4 vertices.
   Each vertex should be a vector of 3 coordinates [x y z]"
  [name vertices]
  (validate-vertices vertices 4)
  (str "in " name " arb4 " (vertices->string vertices)))

(defn insert-arb5
  "Create an ARB5 primitive with 5 vertices"
  [name vertices]
  (validate-vertices vertices 5)
  (str "in " name " arb5 " (vertices->string vertices)))

(defn insert-arb6
  "Create an ARB6 primitive with 6 vertices"
  [name vertices]
  (validate-vertices vertices 6)
  (str "in " name " arb6 " (vertices->string vertices)))

(defn insert-arb7
  "Create an ARB7 primitive with 7 vertices"
  [name vertices]
  (validate-vertices vertices 7)
  (str "in " name " arb7 " (vertices->string vertices)))

(defn insert-arb8
  "Create an ARB8 primitive with 8 vertices"
  [name vertices]
  (validate-vertices vertices 8)
  (str "in " name " arb8 " (vertices->string vertices)))