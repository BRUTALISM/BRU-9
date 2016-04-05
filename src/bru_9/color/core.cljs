(ns bru-9.color.core
  (:require [thi.ng.color.core :as c]))

(def palettes
  (let [convert (fn [cols] (map c/css cols))
        pals
        {:maller ["#f21d6b" "#f2358d" "#35d0f2" "#f29e38" "#f23535"]
         :div ["#031431" "#3C7DB7" "#F7FFFF" "#D10013" "#FB0B00"]
         :2013_200 ["#181E3E" "#F14245" "#F39C3F" "#F8D230" "#743653" "#28234B"
                    "#592F51" "#432948" "#BF2F4C" "#332676"]
         :beeple ["#2CA3DF" "#2D93D2" "#2E83B7" "#31BBEC" "#484631" "#3D6F95"
                  "#57595D" "#93C9E9" "#C07E9C" "#F16717"]}]
    (zipmap (keys pals) (map convert (vals pals)))))

(defn random-palette
  "Gets a random palette from the palette map."
  []
  ((rand-nth (keys palettes)) palettes))
