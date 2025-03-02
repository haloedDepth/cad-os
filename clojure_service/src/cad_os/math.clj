(ns cad-os.math
  (:require [clojure.string :as str]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; Define a consistent numeric precision for mged
(def ^:private precision 6)

;; ------ Number formatting ------

(defn normalize-number
  "Normalize a number to a consistent format for mged.
   Ensures all numbers sent to mged have consistent precision."
  [num]
  (cond
    (integer? num) (double num)  ;; Convert integers to doubles for consistency
    (float? num) (double num)    ;; Ensure float is double precision
    (ratio? num) (double num)    ;; Convert rationals to doubles
    (double? num) num            ;; Already a double
    :else (do
            ((:warn log) "Unexpected number type, converting to double" {:value num :type (type num)})
            (double num))))      ;; Try to convert anything else to double

(defn format-number
  "Format a number to a consistent string representation for mged."
  [num]
  (format (str "%." precision "f") (normalize-number num)))

;; ------ Point operations ------

(defn create-point
  "Create a 3D point from individual x, y, z coordinates.
   This helps make code more explicit when creating points."
  [x y z]
  [(normalize-number x) (normalize-number y) (normalize-number z)])

(defn point->str
  "Convert a 3D point vector to a consistently formatted string."
  [[x y z]]
  (str (format-number x) " " (format-number y) " " (format-number z)))

(defn validate-point
  "Validate that v is a 3D point vector"
  [v]
  (when-not (and (vector? v) (= (count v) 3) (every? number? v))
    ((:error log) "Invalid point vector" {:point v})
    (throw (Exception. (str "Expected a vector of 3 numbers [x y z], got: " v)))))

(defn validate-points
  "Validate multiple 3D point vectors"
  [points]
  (doseq [p points]
    (validate-point p)))

(defn normalize-point
  "Normalize each component of a 3D point vector."
  [point]
  (validate-point point)
  (mapv normalize-number point))

;; ------ Vector operations ------

(defn vec-magnitude
  "Calculate the magnitude (length) of a vector."
  [v]
  (validate-point v)
  (let [[x y z] (mapv normalize-number v)]
    (Math/sqrt (+ (* x x) (* y y) (* z z)))))

(defn normalize-vector
  "Normalize a vector to unit length."
  [v]
  (validate-point v)
  (let [mag (vec-magnitude v)]
    (if (zero? mag)
      [0.0 0.0 0.0]
      (mapv #(/ (normalize-number %) mag) v))))

(defn vec-add
  "Add two vectors."
  [v1 v2]
  (validate-point v1)
  (validate-point v2)
  (mapv + (mapv normalize-number v1) (mapv normalize-number v2)))

(defn vec-subtract
  "Subtract v2 from v1."
  [v1 v2]
  (validate-point v1)
  (validate-point v2)
  (mapv - (mapv normalize-number v1) (mapv normalize-number v2)))

(defn vec-scale
  "Scale a vector by a scalar value."
  [v scalar]
  (validate-point v)
  (let [s (normalize-number scalar)]
    (mapv #(* (normalize-number %) s) v)))

(defn vec-dot
  "Calculate the dot product of two vectors."
  [v1 v2]
  (validate-point v1)
  (validate-point v2)
  (let [[x1 y1 z1] (mapv normalize-number v1)
        [x2 y2 z2] (mapv normalize-number v2)]
    (+ (* x1 x2) (* y1 y2) (* z1 z2))))

(defn vec-cross
  "Calculate the cross product of two vectors."
  [v1 v2]
  (validate-point v1)
  (validate-point v2)
  (let [[x1 y1 z1] (mapv normalize-number v1)
        [x2 y2 z2] (mapv normalize-number v2)]
    [(- (* y1 z2) (* z1 y2))
     (- (* z1 x2) (* x1 z2))
     (- (* x1 y2) (* y1 x2))]))

;; ------ Distance calculations ------

(defn distance
  "Calculate the distance between two points."
  [p1 p2]
  (validate-point p1)
  (validate-point p2)
  (vec-magnitude (vec-subtract p2 p1)))

;; ------ Angle calculations ------

(defn angle-between
  "Calculate the angle between two vectors in radians."
  [v1 v2]
  (validate-point v1)
  (validate-point v2)
  (let [dot (vec-dot v1 v2)
        mag1 (vec-magnitude v1)
        mag2 (vec-magnitude v2)]
    (if (or (zero? mag1) (zero? mag2))
      0.0
      (Math/acos (/ dot (* mag1 mag2))))))

(defn degrees->radians
  "Convert an angle from degrees to radians."
  [degrees]
  (let [deg (normalize-number degrees)]
    (* deg (/ Math/PI 180.0))))

(defn radians->degrees
  "Convert an angle from radians to degrees."
  [radians]
  (let [rad (normalize-number radians)]
    (* rad (/ 180.0 Math/PI))))

;; ------ Rotation and transformation ------

(defn rotate-point
  "Rotate a point around an axis by an angle in radians."
  [point axis angle]
  (validate-point point)
  (validate-point axis)
  (let [normalized-axis (normalize-vector axis)
        [ax ay az] normalized-axis
        cos-angle (Math/cos angle)
        sin-angle (Math/sin angle)
        [px py pz] (mapv normalize-number point)

        ;; Rodrigues rotation formula
        dot-prod (* (vec-dot point normalized-axis) (- 1 cos-angle))
        cross-prod (vec-cross normalized-axis point)

        rx (+ (* px cos-angle) (* (- (* ay pz) (* az py)) sin-angle) (* ax dot-prod))
        ry (+ (* py cos-angle) (* (- (* az px) (* ax pz)) sin-angle) (* ay dot-prod))
        rz (+ (* pz cos-angle) (* (- (* ax py) (* ay px)) sin-angle) (* az dot-prod))]

    [rx ry rz]))

;; ------ Matrix operations ------

(defn create-identity-matrix
  "Create a 4x4 identity matrix for 3D transformations."
  []
  [[1.0 0.0 0.0 0.0]
   [0.0 1.0 0.0 0.0]
   [0.0 0.0 1.0 0.0]
   [0.0 0.0 0.0 1.0]])

(defn matrix-multiply
  "Multiply two matrices."
  [m1 m2]
  (let [rows1 (count m1)
        cols1 (count (first m1))
        rows2 (count m2)
        cols2 (count (first m2))]
    (when-not (= cols1 rows2)
      (throw (Exception. (str "Cannot multiply " rows1 "x" cols1 " and " rows2 "x" cols2 " matrices"))))

    (vec
     (for [i (range rows1)]
       (vec
        (for [j (range cols2)]
          (reduce + (map * (nth m1 i) (map #(nth % j) m2)))))))))

(defn transform-point
  "Transform a point using a 4x4 transformation matrix."
  [matrix point]
  (validate-point point)
  (let [[x y z] (mapv normalize-number point)
        point-vec [x y z 1.0]
        result (vec (first (matrix-multiply [point-vec] matrix)))]
    [(nth result 0) (nth result 1) (nth result 2)]))

;; ------ Translation, rotation, and scaling matrices ------

(defn translation-matrix
  "Create a 4x4 translation matrix."
  [[tx ty tz]]
  [[1.0 0.0 0.0 tx]
   [0.0 1.0 0.0 ty]
   [0.0 0.0 1.0 tz]
   [0.0 0.0 0.0 1.0]])

(defn scale-matrix
  "Create a 4x4 scaling matrix."
  [[sx sy sz]]
  [[sx  0.0 0.0 0.0]
   [0.0 sy  0.0 0.0]
   [0.0 0.0 sz  0.0]
   [0.0 0.0 0.0 1.0]])

(defn rotation-x-matrix
  "Create a 4x4 rotation matrix around the X axis."
  [angle]
  (let [cos-angle (Math/cos angle)
        sin-angle (Math/sin angle)]
    [[1.0 0.0       0.0        0.0]
     [0.0 cos-angle (- sin-angle) 0.0]
     [0.0 sin-angle cos-angle  0.0]
     [0.0 0.0       0.0        1.0]]))

(defn rotation-y-matrix
  "Create a 4x4 rotation matrix around the Y axis."
  [angle]
  (let [cos-angle (Math/cos angle)
        sin-angle (Math/sin angle)]
    [[cos-angle  0.0 sin-angle 0.0]
     [0.0        1.0 0.0       0.0]
     [(- sin-angle) 0.0 cos-angle 0.0]
     [0.0        0.0 0.0       1.0]]))

(defn rotation-z-matrix
  "Create a 4x4 rotation matrix around the Z axis."
  [angle]
  (let [cos-angle (Math/cos angle)
        sin-angle (Math/sin angle)]
    [[cos-angle (- sin-angle) 0.0 0.0]
     [sin-angle cos-angle  0.0 0.0]
     [0.0       0.0        1.0 0.0]
     [0.0       0.0        0.0 1.0]]))

(defn rotation-axis-matrix
  "Create a 4x4 rotation matrix around an arbitrary axis."
  [axis angle]
  (validate-point axis)
  (let [[ax ay az] (normalize-vector axis)
        cos-angle (Math/cos angle)
        sin-angle (Math/sin angle)
        t (- 1 cos-angle)

        ;; Calculate rotation matrix elements
        m00 (+ cos-angle (* ax ax t))
        m01 (- (* ax ay t) (* az sin-angle))
        m02 (+ (* ax az t) (* ay sin-angle))

        m10 (+ (* ay ax t) (* az sin-angle))
        m11 (+ cos-angle (* ay ay t))
        m12 (- (* ay az t) (* ax sin-angle))

        m20 (- (* az ax t) (* ay sin-angle))
        m21 (+ (* az ay t) (* ax sin-angle))
        m22 (+ cos-angle (* az az t))]

    [[m00 m01 m02 0.0]
     [m10 m11 m12 0.0]
     [m20 m21 m22 0.0]
     [0.0 0.0 0.0 1.0]]))