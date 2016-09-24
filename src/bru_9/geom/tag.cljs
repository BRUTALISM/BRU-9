(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.rect :as rect]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a colored
  mesh, writing it into the given accumulator mesh."
  [acc tag color]
  (let [num-points 10
        offset (v/randvec3)
        point-iterator (fn [p] (m/+ (m/+ p offset) (v/randvec3 0.2)))
        lazy-points (iterate point-iterator (v/randvec3 (rand 10)))
        points (take num-points lazy-points)
        shoot (v/vec3 0.0 0.0 1.0)
        profilefn (fn []
                    (map #(if (< (rand) 0.5) (m/+ shoot %) %)
                         (map v/vec3 (g/vertices (rect/rect 0 0 0.5 2)))))
        profiles (take num-points (repeatedly profilefn))
        colorfn (fn [] (c/random-analog color 0.3))
        colors (attr/const-face-attribs (repeatedly colorfn))
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points profiles sweep-params)))
