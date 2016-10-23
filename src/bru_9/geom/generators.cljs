(ns bru-9.geom.generators
  (:require [thi.ng.math.core :as m]
            [thi.ng.geom.vector :as v]
            [bru-9.util :as u]
            [bru-9.field.linear :as fl]
            [bru-9.field.core :as f]
            [bru-9.geom.bezier :as b]
            [thi.ng.math.noise :as n]))

; Highly tweaked generation logic – sculptural, not general.

(defn field-generator [coords params]
  (let [{:keys [random-intensity direction noise-offset
                noise-multiplier]} params
        noise-scale 5.0
        noise-coords (map #(+ noise-offset (/ % noise-scale)) coords)
        noise (-> (apply n/noise3 noise-coords)
                  (* noise-multiplier))]
    (-> direction
        (m/+ (v/randvec3 random-intensity))
        (m/* (+ 1.0 noise)))))

(defn make-directions [initial count]
  (take count (iterate #(m/- %) initial)))

(defn- noise-offset [] (rand 100))

(defn make-fields [count dimensions direction random-intensity noise]
  (let [dirs (make-directions direction count)
        fgen (fn [coords dir]
               (field-generator coords {:random-intensity random-intensity
                                        :direction dir
                                        :noise-offset (noise-offset)
                                        :noise-multiplier noise}))
        constructor (fn [dir] (fl/linear-field dimensions #(fgen % dir)))]
    (map constructor dirs)))

(defn make-start-positions-field [dimensions direction random-intensity]
  (let [gen #(field-generator % {:random-intensity random-intensity
                                 :direction direction
                                 :noise-offset (noise-offset)
                                 :noise-multiplier 0.0})]
    (fl/linear-field dimensions gen)))

(defn make-start-positions [field hops mulfn]
  (f/walk field v/V3 hops mulfn))

(defn make-field-splines
  "Returns a collection of splines generated by walking the given fields."
  [fields start-positions mulfn config]
  (let [{:keys [spline-hops-min
                spline-hops-max
                start-positions-random-offset
                curve-tightness-min
                curve-tightness-max
                wander-probability]} config
        offsetfn
        (fn [pos]
          (m/+ pos (apply v/vec3
                          (repeatedly 3 #(* (u/rand-normal)
                                            start-positions-random-offset)))))
        offset-starts (map offsetfn start-positions)]
    (map #(b/spline-wander fields
                           %
                           (u/rand-int-range spline-hops-min
                                             (inc spline-hops-max))
                           mulfn
                           (u/rand-range curve-tightness-min
                                         curve-tightness-max)
                           wander-probability)
         offset-starts)))