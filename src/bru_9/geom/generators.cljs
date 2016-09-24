(ns bru-9.geom.generators
  (:require [thi.ng.math.core :as m]
            [thi.ng.geom.vector :as v]
            [bru-9.util :as u]
            [bru-9.field.linear :as fl]
            [bru-9.field.core :as f]
            [bru-9.geom.bezier :as b]))

; Highly tweaked generation logic – sculptural, not general.

(defn field-generator [_ random-intensity direction]
  (m/+ direction (v/randvec3 random-intensity)))

(defn make-directions [initial count]
  (loop [dirs [initial], i count]
    (if (> i 0)
      (recur (conj dirs (m/- (last dirs))) (dec i))
      (rest dirs))))

(defn make-fields [count dimensions direction random-follow]
  (let [dirs (make-directions direction count)
        fgen (fn [_ dir] (field-generator _ random-follow dir))
        constructor (fn [dir] (fl/linear-field dimensions #(fgen % dir)))]
    (map constructor dirs)))

(defn make-start-positions-field [dimensions direction random-intensity]
  (fl/linear-field dimensions #(field-generator % random-intensity direction)))

(defn make-start-positions [field hops mulfn]
  (f/walk field v/V3 hops mulfn))

(defn make-field-splines
  "Returns a collection of splines generated by walking the given fields."
  [fields start-positions mulfn config]
  (let [{:keys [spline-hops
                offset-radius
                curve-tightness-min
                curve-tightness-max
                wander-probability]} config
        offset-positions (map #(m/+ % (v/randvec3 offset-radius))
                              start-positions)]
    (map #(b/spline-wander fields % (+ 2 (rand-int (- spline-hops 2))) mulfn
                           (u/rand-range curve-tightness-min
                                         curve-tightness-max)
                           wander-probability)
         offset-positions)))