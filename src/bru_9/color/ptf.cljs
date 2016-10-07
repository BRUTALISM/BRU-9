(ns bru-9.color.ptf
  (:require [thi.ng.geom.attribs :as attr]
            [thi.ng.math.core :as m]))

(defn gradient-ordering
  "Returns PTF color ordering (a sequence of per-face vertex colors) for making
  a gradient from from-color to to-color when transporting a profile with
  face-count edges in one PTF step (e.g. one \"ring\" of faces)."
  [from-color to-color face-count]
  (repeat face-count [from-color from-color to-color to-color]))

(defn ptf-gradient-attribs
  "Generates face color attributes for PTFing a profile with the given vertex
  count, generating a smooth gradient from from-color to to-color. The steps
  variable is the number of PTF frames onto which the shape will be projected."
  [start-color end-color profile-vertex-count steps]
  (let [cfn (fn [i]
              (let [t0 (/ i steps)
                    t1 (/ (inc i) steps)
                    c0 (m/mix start-color end-color t0)
                    c1 (m/mix start-color end-color t1)]
                (gradient-ordering c0 c1 profile-vertex-count)))]
    (attr/face-attribs (mapcat cfn (range steps)))))
