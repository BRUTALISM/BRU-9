(ns bru-9.geom.brush
  (:require [thi.ng.math.core :as m]
            [thi.ng.math.noise :as n]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.circle :as cir]))

; A brush returns the shape which will be projected along a spline. It should
; take at least two parameters as input. The first parameter t represents the
; percentage (in the [0, 1] range) of how far along the curve the current shape
; is. The second parameter is the size of the brush.

(defn sine [t size offset vertices power]
  (g/vertices
    (cir/circle (* size (u/pow (u/sin (+ offset (* t offset))) power)))
    vertices))

(defn two-sided-spikes [t size vertices]
  (sine t size m/PI vertices 2))

(defn noise-spikes [t size vertex-count]
  (let [enveloped-amplitude (* size (n/noise1 t))]
    (sine t enveloped-amplitude m/PI vertex-count 2)))

(defn rotating-quad [t size angle]
  (let [a (* t angle)
        radius size ;(* size (u/pow (u/sin (* t m/PI)) 2))
        shape (g/rotate (cir/circle radius) a)]
    (g/vertices shape 4)))

(defn noise-quad [t size]
  (let [radius (* size (n/noise1 t))
        shape (cir/circle radius)]
    (g/vertices shape 4)))