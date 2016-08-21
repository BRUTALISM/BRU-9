(ns bru-9.geom.brush
  (:require [thi.ng.math.core :as m]
            [thi.ng.math.noise :as n]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.circle :as cir]))

; A brush returns the shape which will be projected along a spline. It can, but
; does not have to, take a parameter t as input, which represents the percentage
; (in the [0, 1] range) of how far along the curve the current shape is.

(defn random-squares [amplitude]
  (g/vertices (cir/circle (* amplitude (rand))) 4))

(defn sine [t amplitude offset vertices power]
  (g/vertices
    (cir/circle (* amplitude (u/pow (u/sin (+ offset (* t offset))) power)))
    vertices))

(defn two-sided-spikes [t amplitude vertices]
  (sine t amplitude m/PI vertices 2))

(defn noise-spikes [t amplitude vertex-count]
  (let [enveloped-amplitude (* amplitude (n/noise1 t))]
    (sine t enveloped-amplitude m/PI vertex-count 2)))

(defn rotating-quad [t amplitude angle]
  (let [a (* t angle)
        radius (* amplitude (u/pow (u/sin (* t m/PI)) 2))
        shape (g/rotate (cir/circle radius) a)]
    (g/vertices shape 4)))