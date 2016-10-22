(ns bru-9.color.core
  (:require [thi.ng.color.core :as tc]
            [bru-9.util :as u]
            [thi.ng.math.core :as m]))

(def palettes
  (let [convert (fn [cols] (map tc/css cols))
        pals
        {
         ;:tar ["#000000"]
         ;:santa ["#FFFFFF"]

         ;:ohai ["#3784FF"]
         ;:ball ["#F44677"]
         ;:to ["#FC3B36"]
         ;:wer ["#2AB4CD"]

         :ballin ["#2055CD" "#F44677"]
         :tower ["#FC3B36" "#2AB4CD"]
         :swet ["#3C7CE6" "#F8746D"]
         :flume ["#FE639C" "#913597"]

         ;:maller ["#f21d6b" "#f2358d" "#35d0f2" "#f29e38" "#f23535"]
         ;:div ["#031431" "#3C7DB7" "#F7AA11" "#D10013" "#FB0B00"]
         ;:2013_200 ["#181E3E" "#F14245" "#F39C3F" "#F8D230" "#743653" "#28234B"
         ;           "#592F51" "#432948" "#BF2F4C" "#332676"]
         ;:beeple ["#2CA3DF" "#2D93D2" "#2E83B7" "#31BBEC" "#484631" "#3D6F95"
         ;         "#57595D" "#93C9E9" "#C07E9C" "#F16717"]
         ;:best2016_1 ["#99B898" "#FECEA8" "#FF847C" "#E84A5F" "#2A363B"]
         ;:best2016_2 ["#ABA7A8" "#CC527A" "#E8175D" "#474747" "#363636"]
         ;:best2016_3 ["#A6206A" "#EC1C4B" "#F16A43" "#F7D969" "#2F9395"]
         ;:best2016_4 ["#E5EEC1" "#A2D4AB" "#3EACA8" "#547A82" "#5A5050"]
         ;:best2016_5 ["#F8B195" "#F67280" "#C06C84" "#6C5B7B" "#355C7D"]
         }]
    (zipmap (keys pals) (map convert (vals pals)))))

(defn- avoid-green
  "Takes a hue in the [0, 1] range and maps it to two split ranges, [0, 0.2] and
  [0.4, 1.0]."
  [hue]
  (let [h (+ 0.4 (* hue 0.8))]
    (if (>= h 1.0) (dec h) h)))

(defn- random-hue []
  "Avoids greens."
  (avoid-green (rand)))

(defn random-palette
  "Creates a new base palette where each color will have the corresponding
  brightness from the brightnesses seq."
  [brightnesses hue-range]
  (let [hues (iterate #(avoid-green (+ (* hue-range (m/randnorm)) %))
                      (random-hue))]
    (map (fn [b h] (tc/hsva h (u/rand-range 0.5 1.0) b)) brightnesses hues)))
