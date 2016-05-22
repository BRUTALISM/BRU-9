(ns bru-9.scenes.tbezier
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.debug :as debug]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.bezier :as b]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]))

(defn- field-generator [coords intensity direction]
  (m/+ direction (v/randvec3 intensity)))

(def config {:num-splines 200
             :num-hops 10
             :offset-radius 0.2
             :field (fl/linear-field [5 5 5] #(field-generator % 1.0 v/V3Z))})

(defn- spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops."
  [field startpos hops]
  (b/auto-spline3 (f/walk field startpos hops (+ 0.2 (rand 0.2)))))

(defn- setup-camera [camera]
  (set! (.-x (.-position camera)) 0)
  (set! (.-y (.-position camera)) -14)
  (set! (.-z (.-position camera)) 4)
  (.lookAt camera (THREE.Vector3. 0 0 4)))

(defn setup [initial-context]
  (let [{:keys [num-splines num-hops offset-radius field]} config
        zdim (last (f/dimensions field))
        zcenters (range 0 zdim (/ zdim num-splines))
        centers (map #(m/+ (v/vec3 0 0 %) (v/randvec3 offset-radius)) zcenters)
        colors (ci/infinite-palette (c/random-palette))
        splines (map #(spline-walk field % num-hops) centers)]
    (doseq [[spline color] (map vector splines colors)]
      (debug/line-strip (g/vertices spline) color))
    (setup-camera (:camera initial-context))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
