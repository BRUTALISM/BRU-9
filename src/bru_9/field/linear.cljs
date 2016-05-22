(ns bru-9.field.linear
  (:require [bru-9.field.core :as f]
            [bru-9.util :as u]
            [thi.ng.math.core :as m]))

(defrecord LinearField [vectors]
  f/PField
  (value-at
   [this coords]
   (let [dims (f/dimensions this)
         wrapped-coords (map #(mod (u/abs %1) %2) coords (map dec dims))]
     (u/interpolate-in (:vectors this) wrapped-coords m/+ m/*)))
  (dimensions
   [this]
   (butlast (u/count-dimensions (:vectors this)))))

(defn linear-field
  "Creates an n-dimensional linear vector field. The number of dimensions is
  determined by the length of the resolutions sequence, which should hold an
  integer resolution value for each dimension. For each element in the resulting
  field, the generator function will be called with the current coordinates
  passed to it as a vector. Thus, if you requested a 4-dimensional linear field,
  your generator function should be prepared to receive a sequence with four
  values in it, representing current coordinates."
  [resolutions generator]
  (LinearField. (u/construct-matrix resolutions generator)))
