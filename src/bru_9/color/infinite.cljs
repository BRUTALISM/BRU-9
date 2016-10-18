(ns bru-9.color.infinite
  (:require [thi.ng.color.core :as tc]
            [thi.ng.math.core :as m]
            [bru-9.util :as u]))

(defn next-color
  "Returns the next color in sequence for the given base colors and params."
  [base-colors params]
  (let [{:keys [hue saturation brightness]} params
        {:keys [h s v]} (tc/as-hsva (rand-nth base-colors))
        hue (+ h (* hue (m/randnorm)))
        hue (if (neg? hue) (+ hue 1.0) (if (>= hue 1.0) (- hue 1.0) hue))
        saturation (u/clamp01 (+ s (* saturation (m/randnorm))))
        brightness (u/clamp01 (+ v (* brightness (m/randnorm))))]
    (tc/as-rgba (tc/hsva hue saturation brightness))))

(defn infinite-palette
  "Creates a lazy infinite sequence of colors based off of the given base
  colors. The generation algorithm is configured using the params map."
  ([base-colors] (infinite-palette base-colors {}))
  ([base-colors params]
   (repeatedly #(next-color base-colors params))))
