(ns bru-9.debug
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [>! chan]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.line :as l]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]
            [thi.ng.geom.vector :as v]))

;; Each debug primitive put on the debug channel has the following shape:
;; {:color thi.ng.color
;;  :geom [v1 v2 v3 ... (treated as a line strip, v1 -> v2, v2 -> v3, ...)]}

(defonce channel (chan (async/buffer 65535)))

; TODO: (put! ...) instead of (go (>! ...))
(defn- put [d] (go (>! channel d)))

(defn line
  "Puts a debug line between two given points onto the debug channel."
  ([p1 p2]
   (line p1 p2 c/WHITE))
  ([p1 p2 color]
   (put {:geom (g/vertices (l/line3 p1 p2))
         :color color})))

(defn line-strip
  "Puts ps (a vector of Vec3s) onto the debug channel as a debug line strip. The
  line will be drawn as p1 -> p2, p2 -> p3, etc."
  ([ps]
   (line-strip ps c/WHITE))
  ([ps color]
   (put {:geom ps
         :color color})))

(defn arrow
  "Puts a debug arrow between two given points (p1 -> p2) onto the debug
  channel."
  ([p1 p2] (arrow p1 p2 c/WHITE))
  ;; TODO: Implement properly.
  ([p1 p2 color] (line p1 p2 color)))

(defn cube
  "Puts a debug cube with the given center and size (both Vec3) onto the debug
  channel."
  ([center size] (cube center size c/WHITE))
  ([center size color]
   (let [[cx cy cz] (map center [:x :y :z])
         [sx sy sz] (map #(/ (size %) 2) [:x :y :z])
         p0 (v/vec3 (- cx sx) (- cy sy) (- cz sz))
         p1 (v/vec3 (+ cx sx) (- cy sy) (- cz sz))
         p2 (v/vec3 (+ cx sx) (- cy sy) (+ cz sz))
         p3 (v/vec3 (- cx sx) (- cy sy) (+ cz sz))
         p4 (v/vec3 (- cx sx) (+ cy sy) (- cz sz))
         p5 (v/vec3 (+ cx sx) (+ cy sy) (- cz sz))
         p6 (v/vec3 (+ cx sx) (+ cy sy) (+ cz sz))
         p7 (v/vec3 (- cx sx) (+ cy sy) (+ cz sz))]
     (line-strip [p0 p1 p2 p3 p0
                  p4
                  p5 p6 p7 p4
                  p5 p1 p2 p6 p7 p3] color))))
