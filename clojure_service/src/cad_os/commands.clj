(ns cad-os.commands
  (:require [clojure.string :as str]
            [cad-os.utils.logger :as logger]
            [cad-os.math :as math]))

;; Initialize logger
(def log (logger/get-logger))

;; Helper functions - this is now just an alias to maintain backward compatibility
(defn vec->str
  "Convert a vector of numbers to space-separated string"
  [v]
  (str/join " " (map math/format-number v)))

(defn point->str
  "Convert a 3D point vector to space-separated string"
  [point]
  (math/point->str point))

(defn create-point
  "Create a 3D point from individual x, y, z coordinates."
  [x y z]
  (math/create-point x y z))

(defn validate-point
  "Validate that v is a 3D point vector"
  [v]
  (math/validate-point v))

(defn validate-points
  "Validate multiple 3D point vectors"
  [points]
  (math/validate-points points))

;; Ellipsoids
(defn insert-ellipsoid
  "Create ellipsoid using center and three vectors"
  [name center front right up]
  (math/validate-points [center front right up])
  ((:debug log) "Inserting ellipsoid" {:name name})
  (str "in " name " ell "
       (math/point->str center) " "
       (math/point->str front) " "
       (math/point->str right) " "
       (math/point->str up)))

(defn insert-sphere
  "Create sphere using center point and radius"
  [name center radius]
  (math/validate-point center)
  ((:debug log) "Inserting sphere" {:name name :radius radius})
  (str "in " name " sph " (math/point->str center) " " (math/format-number radius)))

(defn insert-ellipsoid-g
  "Create ellipsoid using two foci and axis length"
  [name focus1 focus2 axis-length]
  (math/validate-points [focus1 focus2])
  ((:debug log) "Inserting ellipsoid-g" {:name name})
  (str "in " name " ellg "
       (math/point->str focus1) " "
       (math/point->str focus2) " "
       (math/format-number axis-length)))

(defn insert-ellipsoid-1
  "Create ellipsoid using vertex, vector, and revolution radius"
  [name vertex vector-a radius-revolution]
  (math/validate-points [vertex vector-a])
  ((:debug log) "Inserting ellipsoid-1" {:name name})
  (str "in " name " ell1 "
       (math/point->str vertex) " "
       (math/point->str vector-a) " "
       (math/format-number radius-revolution)))

(defn insert-elliptical-hyperboloid
  "Create elliptical hyperboloid"
  [name vertex vector-h vector-a magnitude-b apex-distance]
  (math/validate-points [vertex vector-h vector-a])
  ((:debug log) "Inserting elliptical hyperboloid" {:name name})
  (str "in " name " ehy "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-a) " "
       (math/format-number magnitude-b) " " (math/format-number apex-distance)))

(defn insert-elliptical-paraboloid
  "Create elliptical paraboloid"
  [name vertex vector-h vector-a magnitude-b]
  (math/validate-points [vertex vector-h vector-a])
  ((:debug log) "Inserting elliptical paraboloid" {:name name})
  (str "in " name " epa "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-a) " "
       (math/format-number magnitude-b)))

;; Cones and cylinders
(defn insert-truncated-general-cone
  "Create truncated general cone"
  [name vertex vector-h vector-a vector-b magnitude-c magnitude-d]
  (math/validate-points [vertex vector-h vector-a vector-b])
  ((:debug log) "Inserting truncated general cone" {:name name})
  (str "in " name " tgc "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-a) " "
       (math/point->str vector-b) " "
       (math/format-number magnitude-c) " " (math/format-number magnitude-d)))

(defn insert-right-circular-cylinder
  "Create right circular cylinder."
  [name position vector-h radius]
  (math/validate-points [position vector-h])
  ((:debug log) "Inserting right circular cylinder"
                {:name name
                 :position position
                 :height vector-h
                 :radius radius})
  (str "in " name " rcc "
       (math/point->str position) " "
       (math/point->str vector-h) " "
       (math/format-number radius)))

(defn insert-right-elliptical-cylinder
  "Create right elliptical cylinder"
  [name position vector-h radius]
  (math/validate-points [position vector-h])
  ((:debug log) "Inserting right elliptical cylinder" {:name name})
  (str "in " name " rec "
       (math/point->str position) " "
       (math/point->str vector-h) " "
       (math/format-number radius)))

(defn insert-right-hyperbolic-cylinder
  "Create right hyperbolic cylinder"
  [name vertex vector-h vector-b rectangular-half-width apex-distance]
  (math/validate-points [vertex vector-h vector-b])
  ((:debug log) "Inserting right hyperbolic cylinder" {:name name})
  (str "in " name " rhc "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-b) " "
       (math/format-number rectangular-half-width) " " (math/format-number apex-distance)))

(defn insert-right-parabolic-cylinder
  "Create right parabolic cylinder"
  [name vertex vector-h vector-b rectangular-half-width]
  (math/validate-points [vertex vector-h vector-b])
  ((:debug log) "Inserting right parabolic cylinder" {:name name})
  (str "in " name " rpc "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-b) " "
       (math/format-number rectangular-half-width)))

(defn insert-truncated-elliptical-cone
  "Create truncated elliptical cone"
  [name vertex vector-h vector-a vector-b]
  (math/validate-points [vertex vector-h vector-a vector-b])
  ((:debug log) "Inserting truncated elliptical cone" {:name name})
  (str "in " name " tec "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-a) " "
       (math/point->str vector-b)))

(defn insert-truncated-right-circular-cone
  "Create truncated right circular cone"
  [name vertex vector-h radius-base radius-top]
  (math/validate-points [vertex vector-h])
  ((:debug log) "Inserting truncated right circular cone" {:name name})
  (str "in " name " trc "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/format-number radius-base) " " (math/format-number radius-top)))

;; Other solids
(defn insert-torus
  "Create torus"
  [name center normal radius-revolution radius-tube]
  (math/validate-points [center normal])
  ((:debug log) "Inserting torus" {:name name})
  (str "in " name " tor "
       (math/point->str center) " "
       (math/point->str normal) " "
       (math/format-number radius-revolution) " " (math/format-number radius-tube)))

(defn insert-elliptical-torus
  "Create elliptical torus"
  [name center normal radius-revolution vector-c magnitude-semi-minor-axis]
  (math/validate-points [center normal vector-c])
  ((:debug log) "Inserting elliptical torus" {:name name})
  (str "in " name " eto "
       (math/point->str center) " "
       (math/point->str normal) " "
       (math/format-number radius-revolution) " "
       (math/point->str vector-c) " "
       (math/format-number magnitude-semi-minor-axis)))

(defn insert-conical-particle
  "Create conical particle"
  [name vertex vector-h radius-v radius-h]
  (math/validate-points [vertex vector-h])
  ((:debug log) "Inserting conical particle" {:name name})
  (str "in " name " part "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/format-number radius-v) " " (math/format-number radius-h)))

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
      (str "{carc S " start " E " end " R " (math/format-number radius) " L " left-right " O " orientation "}")
      (do
        ((:error log) "Invalid arc parameters"
                      {:errors (:errors validation)
                       :params arc-params})
        (throw (Exception. (str "Invalid arc parameters: "
                                (str/join ", " (:errors validation)))))))))

(defn format-bezier-segment [[degree & points]]
  (if (and (number? degree)
           (>= degree 1)
           (<= degree (count points)))
    (str "{bezier D " (math/format-number degree) " P {" (str/join " " points) "}}")
    (do
      ((:error log) "Invalid bezier parameters"
                    {:degree degree
                     :point-count (count points)})
      (throw (Exception. "Invalid bezier parameters: degree must be between 1 and point count")))))

(defn format-segment [segment]
  (case (first segment)
    :line (format-line-segment (rest segment))
    :arc (format-arc-segment (rest segment))
    :bezier (format-bezier-segment (rest segment))
    (do
      ((:error log) "Unknown segment type" {:type (first segment)})
      (throw (Exception. (str "Unknown segment type: " (first segment)))))))

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
  (math/validate-points [v-point a-point b-point])
  ((:debug log) "Inserting sketch" {:name sketch-name :vertex-count (count vertex-list)})
  (let [validation-result (validate-segments vertex-list segments)]
    (if (:valid validation-result)
      (str "put " sketch-name " sketch "
           "V {" (math/point->str v-point) "} "
           "A {" (math/point->str a-point) "} "
           "B {" (math/point->str b-point) "} "
           "VL { " (str/join " " (map #(str "{" (math/format-number (first %)) " " (math/format-number (second %)) "}") vertex-list)) " } "
           "SL { " (str/join " " (map format-segment segments)) " }")
      (do
        ((:error log) "Sketch validation failed"
                      {:errors (:invalid-segments validation-result)})
        (str "Sketch validation failed: "
             (str/join ", "
                       (map #(str (:error %) " in " (:segment %))
                            (:invalid-segments validation-result))))))))

(defn insert-sketch-revolve
  "Create a sketch revolve"
  [name vertex axis start angle sketch-name]
  (math/validate-points [vertex axis start])
  ((:debug log) "Inserting sketch revolve" {:name name :sketch sketch-name})
  (str "in " name " revolve "
       (math/point->str vertex) " "
       (math/point->str axis) " "
       (math/point->str start) " "
       (math/format-number angle) " " sketch-name))

(defn insert-sketch-extrude
  "Create a sketch extrude"
  [name vertex vector-h vector-a vector-b sketch-name]
  (math/validate-points [vertex vector-h vector-a vector-b])
  ((:debug log) "Inserting sketch extrude" {:name name :sketch sketch-name})
  (str "in " name " extrude "
       (math/point->str vertex) " "
       (math/point->str vector-h) " "
       (math/point->str vector-a) " "
       (math/point->str vector-b) " "
       sketch-name))

;; Boolean operations
(defn union [name shape1 shape2]
  ((:debug log) "Creating union" {:name name :shapes [shape1 shape2]})
  (str "r " name " u " shape1 " u " shape2))

(defn subtraction [name shape1 shape2]
  ((:debug log) "Creating subtraction" {:name name :shapes [shape1 shape2]})
  (str "r " name " u " shape1 " - " shape2))

(defn intersection [name shape1 shape2]
  ((:debug log) "Creating intersection" {:name name :shapes [shape1 shape2]})
  (str "r " name " u " shape1 " + " shape2))

;; Other operations
(defn copy-object [from-object to-object]
  ((:debug log) "Copying object" {:from from-object :to to-object})
  (str "cp " from-object " " to-object))

;; Arbitrary polyhedra
(defn validate-vertices [vertices expected-count]
  (when-not (= (count vertices) expected-count)
    ((:error log) "Invalid vertex count"
                  {:expected expected-count
                   :actual (count vertices)})
    (throw (Exception. (str "Expected " expected-count " vertices, got " (count vertices)))))
  (math/validate-points vertices))

(defn vertices->string [vertices]
  (str/join " " (map math/point->str vertices)))

(defn insert-arb4
  "Create an ARB4 primitive with 4 vertices.
   Each vertex should be a vector of 3 coordinates [x y z]"
  [name vertices]
  (validate-vertices vertices 4)
  ((:debug log) "Inserting ARB4" {:name name})
  (str "in " name " arb4 " (vertices->string vertices)))

(defn insert-arb5
  "Create an ARB5 primitive with 5 vertices"
  [name vertices]
  (validate-vertices vertices 5)
  ((:debug log) "Inserting ARB5" {:name name})
  (str "in " name " arb5 " (vertices->string vertices)))

(defn insert-arb6
  "Create an ARB6 primitive with 6 vertices"
  [name vertices]
  (validate-vertices vertices 6)
  ((:debug log) "Inserting ARB6" {:name name})
  (str "in " name " arb6 " (vertices->string vertices)))

(defn insert-arb7
  "Create an ARB7 primitive with 7 vertices"
  [name vertices]
  (validate-vertices vertices 7)
  ((:debug log) "Inserting ARB7" {:name name})
  (str "in " name " arb7 " (vertices->string vertices)))

(defn insert-arb8
  "Create an ARB8 primitive with 8 vertices"
  [name vertices]
  (validate-vertices vertices 8)
  ((:debug log) "Inserting ARB8" {:name name})
  (str "in " name " arb8 " (vertices->string vertices)))