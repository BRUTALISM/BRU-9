(ns bru-9.geom.tag
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.aabb :as a]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.circle :as circle]
            [bru-9.color.core :as c]))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a mesh,
  writing it into the given accumulator mesh."
  [acc tag]
  ; TODO: temporary
  (let [palette (c/random-palette)]
    (-> (take 3 (repeatedly #(v/randvec3 (rand 10))))
        (ptf/sweep-mesh
         (g/vertices (circle/circle 2) 5)
         {:mesh acc
          :attribs {:col (->> (c/random-palette)
                              (rand-nth)
                              (repeat)
                              (attr/const-face-attribs))}}))
    acc))
