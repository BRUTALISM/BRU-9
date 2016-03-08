(ns bru-9.geom.tags
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v]
            [bru-9.geom.mesh :as m]))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a partial
  mesh."
  [tag]
  (let [shape m/tetrahedron
        offset (v/randvec3 2)
        translated (g/translate shape offset)]
;;     (prn shape)
;;     (prn translated)
;;     translated
    shape
    ))
