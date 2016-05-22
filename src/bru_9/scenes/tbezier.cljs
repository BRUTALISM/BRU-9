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

;; Scene setup & main loop

(def config {:num-splines 40
             :num-hops 10
             :offset-radius 0.2
             :field (fl/linear-field [5 5 5] #(v/randvec3))
             :center (v/vec3 2.5 2.5 2.5)})

(defn- spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops."
  [field startpos hops]
  (b/auto-spline3 (f/walk field startpos hops)))

(defn setup [initial-context]
  (let [{:keys [num-splines num-hops offset-radius center field]} config
        zdim (last (f/dimensions field))
        zcenters (range 0 zdim (/ zdim num-splines))
        centers (map #(m/+ (v/vec3 0 0 %) (v/randvec3 offset-radius)) zcenters)
        colors (ci/infinite-palette (c/random-palette))
        splines (map #(spline-walk field % num-hops) centers)]
    (doseq [[spline color] (map vector splines colors)]
      (debug/line-strip (g/vertices spline) color))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
