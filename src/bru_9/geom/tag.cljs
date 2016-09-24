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
        w (rand 0.2)
        h (rand 0.2)
        v3rect (fn [angle size-multiplier]
                 (let [w (* w size-multiplier)
                       h (* h size-multiplier)]
                   ; TODO: implement rotation
                   (map v/vec3 (g/vertices (rect/rect 0 0 w h)))))
        max-angle (/ m/HALF_PI 2)
        tdiv (dec (count points))
        profilefn (fn [i]
                    (let [t (/ i tdiv)
                          angle (+ (- max-angle) (* 2 t max-angle))
                          size-multiplier (- 1 t)           ; TODO: implement
                          ]
                      (v3rect angle size-multiplier)))
        colorfn (fn [c] (c/adjust-saturation c -0.007))
        colors (attr/const-face-attribs (iterate colorfn color))
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points (map profilefn (range tdiv)) sweep-params)))
