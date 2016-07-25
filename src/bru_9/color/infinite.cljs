(ns bru-9.color.infinite
  (:require [thi.ng.color.core :as tc]))

(defn next-color
  "Returns the next color in sequence for the given base colors and params."
  [colors params]
  (let [{:keys [hue saturation brightness]} params
        base (rand-nth colors)]
    ;; Temporary
    (tc/random-analog base hue saturation brightness)))

(defn infinite-palette
  "Creates a lazy infinite sequence of colors based off of the given base
  colors. The generation algorithm is configured using the params map."
  ([colors] (infinite-palette colors {}))
  ([colors params]
   (repeatedly #(next-color colors params))))
