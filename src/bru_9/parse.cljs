(ns bru-9.parse
  (:require [hickory.core :as hcore]))

(defn level-dom
  "Iterates level-order through each DOM element in the given HTML string,
  returning each node as a parsed structure in Hiccup format."
  [html]
  (let [parsed (-> html hcore/parse hcore/as-hiccup)
        filtered (drop-while #(not (sequential? %)) parsed)]
    (loop [level []
           children [(first filtered)]]
      (if (empty? children)
        level
        (recur (into level children) (mapcat #(drop 2 %) children))))))

(defn occurences
  "Returns all occurences of the given regex in the search-in string. Ignores
  case."
  [search-in regex]
  (map first (re-seq (js/RegExp. regex "i") search-in)))

(defn map-occurences
  "For each string in search-seq, counts its number of occurences in the
  search-in string, and returns a map whose keys are strings from searches and
  values are occurence counts."
  [search-in search-seq]
  (zipmap (map #(keyword %) search-seq)
          (map #(count (occurences search-in %)) search-seq)))
