(ns bru-9.geom.brush
  (:require [thi.ng.math.core :as m]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.circle :as cir]))

; A brush returns the shape which will be projected along a spline. It can, but
; does not have to, take a parameter t as input, which represents the percentage
; (in the [0, 1] range) of how far along the curve the current shape is.

(defn random-squares [amplitude]
  (g/vertices (cir/circle (* amplitude (rand))) 4))

(defn sine [t amplitude offset vertices]
  (g/vertices
    (cir/circle (* amplitude (u/sin (+ offset (* t offset)))))
    vertices))

(defn two-sided-spikes [t amplitude vertices]
  (sine t amplitude m/PI vertices))