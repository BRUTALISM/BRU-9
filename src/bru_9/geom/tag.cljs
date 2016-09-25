(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [bru-9.color.ptf :as cptf]
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
        steps (dec (count points))
        toffset (+ 1 (rand 1))                              ; TODO: temporary
        profilefn (fn [i]
                    (let [t (/ i steps)
                          angle (+ (- max-angle) (* 2 t max-angle))
                          size-multiplier (- toffset t)           ; TODO: implement
                          ]
                      (v3rect angle size-multiplier)))
        colors (cptf/rect-gradient-attribs color (c/random-analog color 0.3) steps)
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points (map profilefn (range steps)) sweep-params)))
