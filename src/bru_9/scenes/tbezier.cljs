(ns bru-9.scenes.tbezier
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.debug :as debug]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.geom.bezier :as b]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]))

(def config {:start-positions-hops 50
             :start-positions-axis-following 0.9
             :start-positions-random-following 1.3
             :start-positions-walk-multiplier 0.04
             :curve-tightness 0.08
             :spline-hops 6
             :offset-radius 0.1
             :field-dimensions [5 5 5]
             :field-directions [(m/normalize (m/+ (v/randvec3) v/V3Z))
                                (m/normalize (m/- (v/randvec3) v/V3Z))]
             :mulfn-base 0.5
             :mulfn-jump-chance 0.2
             :mulfn-jump-intensity 2})

(defn- mulfn [_]
  (let [{:keys [mulfn-base mulfn-jump-chance mulfn-jump-intensity]} config]
    (+ mulfn-base (if (< (rand) mulfn-jump-chance) mulfn-jump-intensity 0))))

(defn- field-generator [_ random-intensity direction]
  (m/+ direction (v/randvec3 random-intensity)))

(defn- make-field [direction]
  (let [random-follow (:start-positions-random-following config)]
    (fl/linear-field (:field-dimensions config)
                     #(field-generator % random-follow direction))))

(defn- make-start-positions [field]
  (let [mulfn (fn [_] (:start-positions-walk-multiplier config))]
    (f/walk field v/V3 (:start-positions-hops config) mulfn)))

(def start-positions
  (->> (:start-positions-axis-following config)
       (m/* v/V3Z)
       make-field
       make-start-positions))

(defn- make-field-splines
  "Returns a collection of splines generated by walking the given field."
  [field]
  (let [{:keys [spline-hops offset-radius curve-tightness]} config
        offset-positions (map #(m/+ % (v/randvec3 offset-radius))
                              start-positions)]
    (map #(b/spline-walk field % spline-hops mulfn curve-tightness)
         offset-positions)))

(defn- draw-field
  "Draws the given field using the given infinite palette."
  [field palette]
  (doseq [[spline color] (map vector (make-field-splines field) palette)]
      (debug/line-strip (g/vertices spline) color)))

(defn- setup-camera [camera]
  (set! (.-x (.-position camera)) 0)
  (set! (.-y (.-position camera)) -30)
  (set! (.-z (.-position camera)) 4)
  (.lookAt camera (THREE.Vector3. 0 0 4)))

(defn setup [initial-context]
  (let [fields (map make-field (:field-directions config))
        palette (c/random-palette)]
    (doseq [field fields]
      (draw-field field (ci/infinite-palette palette)))
    (setup-camera (:camera initial-context))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
