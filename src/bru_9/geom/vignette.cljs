(ns bru-9.geom.vignette
  (:require [thi.ng.color.core :as tc]))

(defn vignette-material [c1 c2]
  (let [[col1 col2] (map #(-> % tc/as-int24 :col) [c1 c2])]
    (THREE.RawShaderMaterial.
      #js {:vertexShader (.-textContent (.getElementById js/document
                                                         "vignette-vertex"))
           :fragmentShader (.-textContent (.getElementById js/document
                                                           "vignette-fragment"))
           :side js/THREE.DoubleSide
           :depthTest false
           :uniforms #js {:aspectCorrection #js {:type "i" :value false}
                          :aspect #js {:type "f" :value 1}
                          :offset #js {:type "v2"
                                       :value (THREE.Vector2. 0 0)}
                          :scale #js {:type "v2"
                                      :value (THREE.Vector2. 1 1)}
                          :smoothness #js {:type "v2"
                                           :value (THREE.Vector2. 0 1)}
                          :color1 #js {:type "c"
                                       :value (THREE.Color. col1)}
                          :color2 #js {:type "c"
                                       :value (THREE.Color. col2)}}})))

(defn setup-vignette [camera color1 color2]
  (let [material (vignette-material color1 color2)
        geometry (THREE.PlaneGeometry. 60 30)
        plane (THREE.Mesh. geometry material)]
    (set! (.-x (.-position plane)) (.-x (.-position camera)))
    (set! (.-y (.-position plane)) (.-y (.-position camera)))
    (set! (.-z (.-position plane)) -40)
    (.add camera plane)))

(defn adjust-color [color saturation lightness]
  (tc/hsla (tc/hue color) saturation lightness))