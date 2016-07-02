(ns bru-9.geom.bezier
  (:require [bru-9.field.core :as f]
            [thi.ng.geom.bezier :as b]
            [thi.ng.geom.vector :as v]))

(defn auto-spline3
  [points tight]
  (->> points
       (b/find-cpoints* v/vec3 tight)
       (b/auto-spline* points)
       (thi.ng.geom.types.Bezier3.)))

(defn spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops. The mulfn parameter is a
  multiplication function passed to field.core/walk, see the docs there for
  details."
  [field startpos hops mulfn tight]
  (auto-spline3 (f/walk field startpos hops mulfn) tight))

