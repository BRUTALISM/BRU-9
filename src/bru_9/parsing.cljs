(ns bru-9.parsing
  (:require [hickory.core :as hcore]
            [hickory.zip :as hzip]
            [clojure.zip :as zip]))

(defn zip-html [html]
  (-> html
      hcore/parse
      hcore/as-hiccup
      hzip/hiccup-zip))

;(def h "<a><b>1</b><b>2</b><b>3</b></a>")
;(def hz (zip-html h))

(defn depth-seq
  "Iterates depth-first through all DOM elements in the given zipped HTML zh,
  returning a loc at each position"
  [zh]
  (let [first-node (zip/next zh)
        not-end #(not (zip/end? %))]
    (take-while not-end (iterate zip/next first-node))))

(defn level-seq
  "Iterates level-order through all DOM elements in the given zipped HTML zh,
  returning a loc at each position"
  [zh]
  ;; TODO: Implement.
  (let []
    ))
