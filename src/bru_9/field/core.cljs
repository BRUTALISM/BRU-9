(ns bru-9.field.core
  (:require [thi.ng.math.core :as m]))

(defprotocol PField
  "Defines elementary flow-field operations."
  (value-at
   [f position]
   "Returns field value at given position in 3D space.")
  (dimensions
   [f]
   "Returns the sequence of dimensions of the field along each axis."))

(defn walk
  "Returns a sequence of field values whose positions are obtained by fetching
  the field value at a given position (startpos at first), adding the obtained
  value multiplied by the result of calling mulfn to the position, and then
  repeating the process with the new position. This process is repeated hops
  times. The mulfn function should be a function of one parameter which will
  be in the [0, 1] range, representing the percentage of the total number of
  hops the function is being invoked at. This way, you can modulate the speed of
  walking as you do the walking."
  ([f startpos hops] (walk f startpos hops 1))
  ([f startpos hops mulfn]
   (loop [ps [], pos startpos, hopsleft hops]
    (if (> hopsleft 0)
      (recur (conj ps pos)
             (m/+ pos (m/* (value-at f pos)
                           (mulfn (/ (- hops hopsleft) (dec hops)))))
             (dec hopsleft))
      ps))))
