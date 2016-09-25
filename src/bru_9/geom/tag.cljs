(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [bru-9.color.ptf :as cptf]
            [bru-9.util :as u]
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
        h (rand 0.1)
        rotated-rect
        (fn [angle size-multiplier]
          (let [w (* w size-multiplier)
                h (* h size-multiplier)
                zoff (v/vec3 0 0 (* (/ h 2) (u/sin angle)))
                [v0 v1 v2 v3] (map v/vec3 (g/vertices (rect/rect 0 0 w h)))]
            [(m/- v0 zoff)
             (m/- v1 zoff)
             (m/+ v2 zoff)
             (m/+ v3 zoff)]))
        max-angle (/ m/PI 2)
        steps (dec (count points))
        toffset (+ 1 (rand 0.5))                            ; TODO: temporary
        profilefn (fn [i]
                    (let [t (/ i steps)
                          angle (+ (- max-angle) (* 2 t max-angle))
                          size-multiplier (- toffset t)     ; TODO: implement
                          ]
                      (rotated-rect angle size-multiplier)))
        colors (cptf/ptf-gradient-attribs color (c/random-analog color 0.3) 4 steps)
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points (map profilefn (range steps)) sweep-params)))
