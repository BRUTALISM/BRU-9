(ns bru-9.geom.tag
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.aabb :as a]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.circle :as circle]))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a colored
  mesh, writing it into the given accumulator mesh."
  [acc tag color]
  ; TODO: temporary
  (-> (take 3 (repeatedly #(v/randvec3 (rand 10))))
      (ptf/sweep-mesh
       (g/vertices (circle/circle 1) 5)
       {:mesh acc
        :attribs {:col (-> color (repeat) (attr/const-face-attribs))}}))
  acc)
