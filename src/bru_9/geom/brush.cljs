(ns bru-9.geom.brush
  (:require [thi.ng.math.core :as m]
            [bru-9.util :as u]))

(defn two-sided-spikes [t amplitude]
  (* amplitude (u/sin (* t m/PI))))