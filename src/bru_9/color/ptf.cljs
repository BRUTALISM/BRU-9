(ns bru-9.color.ptf
  (:require [thi.ng.geom.attribs :as attr]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]))

(defn gradient-ordering
  "Returns PTF color ordering (a sequence of per-face vertex colors) for making
  a gradient from from-color to to-color when transporting a profile with
  face-count edges in one PTF step (one \"ring\" of faces)."
  [from-color to-color face-count]
  (repeat face-count [from-color from-color to-color to-color]))

(defn rect-gradient-attribs
  "Generates face attribs for PTFing a rectangle along the curve. The final
  shape will have the given start-color and will smoothly interpolate toward
  end-color in the given number of steps."
  [start-color end-color steps]
  (let [cfn (fn [i]
              (let [t0 (/ i steps)
                    t1 (/ (inc i) steps)
                    c0 (m/mix start-color end-color t0)
                    c1 (m/mix start-color end-color t1)]
                (gradient-ordering c0 c1 4)))
        ]
    (attr/face-attribs (mapcat cfn (range steps)))))
