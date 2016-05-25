(ns bru-9.util)

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

(defn frac
  "Returns the result of subtracting (floor x) from x – the part after the
  decimal point."
  [x]
  (- x (floor x)))

(defn sin [x] (.sin js/Math x))
(defn cos [x] (.cos js/Math x))

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
