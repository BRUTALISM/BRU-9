(ns bru-9.geom.bezier
  (:require [bru-9.field.core :as f]
            [bru-9.util :as u]
            [thi.ng.geom.bezier :as b]
            [thi.ng.geom.vector :as v]))

(defn auto-spline3
  "Calculates a Bezier spline from the given points. The bigger the tightness
  parameter, the more the curve will look like a multi-segment straight line."
  [points tightness]
  (->> points
       (b/find-cpoints* v/vec3 tightness)
       (b/auto-spline* points)
       (thi.ng.geom.types.Bezier3.)))

(defn spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops. The mulfn parameter is a
  multiplication function passed to field.core/walk, see the docs there for
  details. The tightness parameter is explained in auto-spline3."
  [field startpos hops mulfn tightness]
  (auto-spline3 (f/walk field startpos hops mulfn) tightness))

(defn spline-wander
  "Returns a bezier spline resulting from walking the given fields by switching
  the source field while it's doing the walking. Behaves similar to spline-walk,
  except that the field being read from is determined by a random roll and the
  freq parameter, which represents the probability that a field will be switched
  before reading the next value on the walk. startpos is the initial starting
  position, hops is the number of samples the function will make, mulfn is
  explained in field.core/walk, and tightness in auto-spline3. The first field
  in the fields seq remains first, others are shuffled."
  [fields startpos hops mulfn tightness freq]
  (let [split-hops (u/random-split hops freq)
        shuffled-fields (cycle (into [(first fields)] (shuffle (rest fields))))
        fields-hops (map vector shuffled-fields split-hops)
        rfn (fn [points [field hops]]
              (let [start (last points)]
                (into points (rest (f/walk field start (inc hops) mulfn)))))]
    (auto-spline3 (reduce rfn [startpos] fields-hops) tightness)))