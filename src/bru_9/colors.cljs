(ns bru-9.colors
  (:require [thi.ng.color.core :as c]))

(def palette
  (let [convert (fn [cols] (map c/css cols))
        palettes
        {:noir ["#111111" "#222222" "#666666" "#aaaaaa" "#ffffff"]
         :maller ["#f21d6b" "#f2358d" "#35d0f2" "#f29e38" "#f23535"]
         :div ["#031431" "#3C7DB7" "#F7FFFF" "#D10013" "#FB0B00"]}]
    (zipmap (keys palettes) (map convert (vals palettes)))))

(defn random-palette
  "Gets a random palette from the palette map."
  []
  ((rand-nth (keys palette)) palette))
