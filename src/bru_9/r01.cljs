(ns bru-9.r01
  (:require [bru-9.util :as u]
            [bru-9.color.core :as c]))

;; GUI init

(def gui-controls {:rotationSpeedX [0 0.005 0.1]
                   :rotationSpeedY [0 0.005 0.1]})

(defn init-gui [control-map]
  (let [controls (u/map2obj (into {} (map #(conj [] (key %) (second (val %)))
                                          control-map)))
        gui (js/dat.GUI.)]
    (doseq [[k [minn _ maxx]] control-map]
      (.add gui controls (name k) minn maxx))

    ;; return a map with initialized gui and controls
    {:gui gui :controls controls}))

;; Mesh generation

(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

(defn random-vectors []
  (let [points-per-mesh 10
        extent 0.2]
    (repeatedly points-per-mesh #(random-vector extent))))

(defn generate-mesh []
  (let [material-properties #js {:color (rand-nth (:maller c/palette))
                                 :shading js/THREE.FlatShading
                                 :transparent true
                                 :opacity 0.9}
        geometry (THREE.ConvexGeometry. (to-array (random-vectors)))
        material (THREE.MeshPhongMaterial. material-properties)
        mesh (THREE.Mesh. geometry material)
        position (random-vector 1.5)]
    ;; setting .-position directly doesn't work, you have to go component by
    ;; component... how lame
    (set! (.-x (.-position mesh)) (.-x position))
    (set! (.-y (.-position mesh)) (.-y position))
    (set! (.-z (.-position mesh)) (.-z position))
    mesh))

(defn generate-meshes []
  (let [mesh-count 20]
    (repeatedly mesh-count generate-mesh)))

(defn generate-background []
  (let [material-properties #js {:color 0x888888
                                 :shading js/THREE.SmoothShading}
        d 20
        geometry (THREE.BoxGeometry. d d d)
        material (THREE.MeshPhongMaterial. material-properties)
        off (- 0 1 (/ d 2))
        positions [(THREE.Vector3. 0 0 off)
                   (THREE.Vector3. off 0 0)
                   (THREE.Vector3. 0 off 0)]
        make-mesh (fn [pos]
                    (let [mesh (THREE.Mesh. geometry material)]
                      (set! (.-x (.-position mesh)) (.-x pos))
                      (set! (.-y (.-position mesh)) (.-y pos))
                      (set! (.-z (.-position mesh)) (.-z pos))
                      mesh))]
    (map make-mesh positions)))

;; Main hook functions

(defn setup [initial-context]
  (let [meshes (generate-meshes)
        background-meshes (generate-background)
        scene (:scene initial-context)]
    (doseq [mesh meshes] (.add scene mesh))
    (doseq [mesh background-meshes] (.add scene mesh))
    (conj initial-context
          {:meshes meshes
           :background background-meshes}
          (init-gui gui-controls))))

(defn animate [context]
  (let [meshes (:meshes context)
        controls (:controls context)]
    (doseq [mesh meshes]
      (set! (.-x (.-rotation mesh))
            (+ (.-x (.-rotation mesh)) (.-rotationSpeedX controls)))
      (set! (.-y (.-rotation mesh)) (+ (.-y (.-rotation mesh))
                                       (.-rotationSpeedY controls))))
    context))
