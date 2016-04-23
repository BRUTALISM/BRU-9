(ns bru-9.field.linear
  (:require [bru-9.field.core :as f]
            [bru-9.util :as u]
            [thi.ng.geom.vector :as v]))

(defrecord LinearField [vectors]
  f/PField
  (value-at
   [this coords]
   ;; TODO: Interpolation.
   (get-in (:vectors this) coords)))

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
