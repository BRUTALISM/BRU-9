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

;; (def h "<a><b>1</b><b></b><b>3</b></a><c><d>4</d><d>5</d></c>")
