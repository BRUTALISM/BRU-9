(ns bru-9.geom.tags
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.aabb :as a]
            [thi.ng.geom.core.vector :as v]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.color.core :as c]))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a mesh,
  writing it into the given accumulator mesh."
  [acc tag]
  ; TODO: temporary
  (-> (a/aabb 1)
      (g/center)
      (g/translate (v/randvec3 2))
      (g/as-mesh
       {:mesh    acc
        :attribs {:col (->> [[1 0 0] [0 1 0] [0 0 1]
                             [0 1 1] [1 0 1] [1 1 0]]
                            (map c/rgba)
                            (attr/const-face-attribs))}}))
  acc)
