(ns bru-9.util
  (:require [thi.ng.math.core :as m]))

(defn map2obj [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn nths
  "Returns a collection of values at given indices"
  [coll idxs]
  (map (partial nth coll) idxs))

(defn indices-of
  "Gets the indices of all occurrences of the given element e in the given
  sequence s"
  [s e]
  (map first (filter #(= e (second %1)) (map-indexed vector s))))

(defn first-index
  "Returns the index of the first occurrence of element e in sequence s"
  [s e]
  (first (indices-of s e)))

(defn abs [x] (.abs js/Math x))
(defn floor [x] (.floor js/Math x))
(defn ceil [x] (.ceil js/Math x))
(defn round [x] (.round js/Math x))
(defn pow [x p] (.pow js/Math x p))
(defn sin [x] (.sin js/Math x))
(defn cos [x] (.cos js/Math x))
(defn atan [x] (.atan js/Math x))
(defn sqrt [x] (.sqrt js/Math x))
(defn log [x] (.log js/Math x))
(defn clamp01 [x] (m/clamp x 0.0 1.0))

(defn rand-range [min max]
  (+ min (rand (- max min))))

(defn rand-normal []
  (let [u (- 1 (rand))
        v (- 1 (rand))]
    (* (sqrt (* -2.0 (log u)))
       (cos (* 2.0 m/PI v)))))

(defn frac
  "Returns the result of subtracting (floor x) from x – the part after the
  decimal point."
  [x]
  (- x (floor x)))

(defn clear-scene [scene]
  (set! (.-children scene) #js []))

(defn construct-matrix
  "Constructs an n-dimensional matrix with sizes for each dimension given by
  the dimensions sequence. Each element in the matrix is initialized by running
  the genfn function which is passed the current coordinates as parameters."
  ([dimensions genfn] (construct-matrix dimensions genfn []))
  ([dimensions genfn coords]
   (let [dim (first dimensions)
         current-range (range dim)
         remaining (vec (rest dimensions))]
     (if (empty? remaining)
       (mapv #(genfn (conj coords %)) current-range)
       (mapv #(construct-matrix remaining genfn (conj coords %))
             current-range)))))

(defn binary-combinations
  "Returns a sequence of all binary combinations for the given width in bits.
  For example: for width = 2, returns ((0 0) (0 1) (1 0) (1 1)); for width = 3,
  returns ((0 0 0) (0 0 1) (0 1 0) ... (1 1 1))."
  [width]
  (let [w0 []
        w1 [[0] [1]]
        w2 [[0 0] [0 1] [1 0] [1 1]]
        w3 [[0 0 0] [0 0 1] [0 1 0] [0 1 1]
            [1 0 0] [1 0 1] [1 1 0] [1 1 1]]
        combinations [w0 w1 w2 w3]]
    (if (and (< width (count combinations))
             (>= width 0))
      (get combinations width)
      (throw (js/Error.
              (str "lazy ass implementation, unsupported width: " width))))))

(defn interpolate-in
  "Behaves like get-in for n-dimensional matrices, where the index to get from
  is a floating point number instead of an integer. The return value is
  interpolated between all neighbouring points using linear interpolation, using
  addfn and mulfn as addition and multiplication functions, respectively."
  [matrix coords addfn mulfn]
  (let [floors (map floor coords)
        ceils (map ceil coords)
        interleaved-limits (map vec (partition 2 (interleave ceils floors)))
        ts (map #(- %1 %2) coords floors)
        omts (map #(- 1 %) ts)
        interleaved-ts (map vec (partition 2 (interleave ts omts)))
        efn (fn [is01]
              (mulfn (get-in matrix (map #(get %1 %2) interleaved-limits is01))
                     (reduce * (map #(get %1 %2) interleaved-ts is01))))
        combinations (binary-combinations (count coords))]
    (reduce addfn (map efn combinations))))

(defn count-dimensions
  "Recursively gets dimensions of the nested sequence. Assumes a fully
  populated (non-sparse) sequence."
  [xs]
  (let [count-fn (fn [s dims]
                   (if (sequential? s)
                     (recur (first s) (conj dims (count s)))
                     dims))]
    (count-fn xs [])))

(defn random-split
  "Splits a given target integer into several random integers which all sum up
  to target. The algorithm counts from zero upward towards target, and at each
  step it performs a random roll telling it whether it should split the current
  count and start a new one. The probability that the split occurs at each step
  is controlled by the prob parameter."
  [target prob]
  (loop [i target, curr 0, ss []]
    (if (> i 0)
      (if (< (rand) prob)
        (recur (dec i) 0 (conj ss (inc curr)))
        (recur (dec i) (inc curr) ss))
      (if (> curr 0)
        (conj ss curr)
        ss))))

(defn nth01
  "Gets from the given collection using the index in the [0, 1] range,
  representing the percentage of the range of the given collection. Returns the
  element which is closest to calculated position."
  [coll t]
  (let [t01 (clamp01 t)
        i (round (* t01 (dec (count coll))))]
    (nth coll i)))

(defn calculate-x-extents [splines]
  (let [extfn
        (fn [[min max] point]
          [(if (< (:x point) (:x min)) point min)
           (if (> (:x point) (:x max)) point max)])
        all-points (mapcat :points splines)]
    (reduce extfn [(first all-points) (first all-points)] all-points)))

(defn rand-range [min max]
  (+ min (rand (- max min))))

(defn rand-int-range [min max]
  (+ min (rand-int (- max min))))

(defn rand-magnitude [val percentage minimum maximum]
  (let [fraction (* val percentage)
        mini (Math/max (double minimum) (- val fraction))
        maxi (Math/min (double maximum) (+ val fraction))]
    (rand-range mini maxi)))

(defn rand-int-magnitude [val magnitude minimum maximum]
  (let [fraction (* (double val) magnitude)
        rounded-fraction (Math/round (Math/max fraction 1.0))
        mini (Math/max (long minimum) (- (long val) rounded-fraction))
        maxi (Math/min (long maximum) (+ (long val) rounded-fraction))]
    (rand-int-range mini maxi)))

(defn saw
  "Returns a symmetrical saw function. The function starts from ybase, rises
  linearly to ymax when t = 0.5, and then starts descending back toward ybase,
  reaching it in t = 1. The only parameter of the returned function is t, which
  is clamped to the [0, 1] range."
  [ybase ymax]
  (fn [t]
    (let [t (clamp01 t)
          height (- ymax ybase)
          coeff (* height 2)]
      (if (< t 0.5)
        (+ ybase (* t coeff))
        (+ ymax (* (- t 0.5)(- coeff)))))))