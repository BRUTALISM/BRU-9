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
  [acc tag spline color spline-resolution]
  (let [points (g/vertices spline spline-resolution)
        shoot (v/vec3 0.0 0.0 0.05)
        profilefn (fn []
                    (map #(if (< (rand) 0.5) (m/+ shoot %) %)
                         (map v/vec3 (g/vertices (rect/rect 0 0 0.05 0.2)))))
        colorfn (fn [] (c/random-analog color 0.3))
        colors (attr/const-face-attribs (repeatedly colorfn))
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points (repeatedly profilefn) sweep-params)))
