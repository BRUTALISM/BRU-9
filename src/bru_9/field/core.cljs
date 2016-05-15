(ns bru-9.field.core
  (:require [thi.ng.math.core :as m]))

(defprotocol PField
  "Defines elementary flow-field operations."
  (value-at
   [f position]
   "Returns field value at given position in 3D space."))

(defn walk
  "Returns a sequence of field values whose positions are obtained by fetching
  the field value at a given position (startpos at first), adding the obtained
  value multiplied by mul to the position, and then repeating the process with
  the new position. This process is repeated hops times."
  ([f startpos hops mul]
   (loop [ps [], pos startpos, hopsleft hops]
    (if (> hopsleft 0)
      (recur (conj ps pos)
             (m/+ pos (m/* (value-at f pos) mul))
             (dec hopsleft))
      ps)))
  ([f startpos hops] (walk f startpos hops 1)))
