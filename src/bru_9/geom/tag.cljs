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
        point-count (count points)
        w (rand 0.2)
        h (rand 0.2)
        v3rect (fn [angle]
                 (map v/vec3 (g/vertices (rect/rect 0 0 w h))))
        max-angle (/ m/HALF_PI 4)
        profilefn (fn [i]
                    (let [t (/ i point-count)
                          angle (+ (- max-angle) (* 2 t max-angle))]
                      (v3rect angle)))
        colorfn (fn [] (c/random-analog color 0.3))
        colors (attr/const-face-attribs (repeatedly colorfn))
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points (map profilefn (range point-count)) sweep-params)))
