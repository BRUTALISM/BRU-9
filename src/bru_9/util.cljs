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
