(ns bru-9.debug
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [>! chan]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.line :as l]
            [thi.ng.color.core :as c]))

;; Each debug primitive put on the debug channel has the following shape:
;; {:color thi.ng.color
;;  :geom [v1 v2 v3 ... (treated as a line strip, v1 -> v2, v2 -> v3, ...)]}

(defonce channel (chan (async/buffer 65535)))

(defn- put [d] (go (>! channel d)))

(defn line
  "Puts a debug line between two given points onto the debug channel."
  ([p1 p2]
   (line p1 p2 c/WHITE))
  ([p1 p2 color]
   (put {:geom (g/vertices (l/line3 p1 p2))
         :color color})))

(defn arrow
  "Puts a debug arrow between two given points (p1 -> p2) onto the debug
  channel."
  ([p1 p2] (arrow p1 p2 c/WHITE))
  ;; TODO: Implement properly.
  ([p1 p2 color] (line p1 p2 color)))

;; TODO: cube, sphere
