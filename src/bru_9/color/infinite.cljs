(ns bru-9.color.infinite
  (:require [bru-9.color.core :as c]))

(defn next-color
  "Returns the next color in sequence for the given base colors and params."
  [colors params]
  (let []
    ;; Temporary
    (rand-nth colors)))

(defn infinite-palette
  "Creates a lazy infinite sequence of colors based off of the given base
  colors. The generation algorithm is configured using the params map."
  ([colors] (infinite-palette colors {}))
  ([colors params]
   (repeatedly #(next-color colors params))))
