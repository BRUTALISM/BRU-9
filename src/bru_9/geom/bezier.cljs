(ns bru-9.geom.bezier
  (:require [thi.ng.geom.bezier :as b]
            [bru-9.field.core :as f]))

(defn- spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops. The mulfn parameter is a
  multiplication function passed to field.core/walk, see the docs there for
  details."
  [field startpos hops mulfn]
  (b/auto-spline3 (f/walk field startpos hops mulfn)))

