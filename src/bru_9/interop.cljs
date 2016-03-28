;; thi.ng -> THREE.js interop functions

(ns bru-9.interop
  (:require [thi.ng.geom.core :as g]
            [thi.ng.typedarrays.core :as ta]
            [thi.ng.color.core :as c]
            [thi.ng.typedarrays.core :as ta]
            [bru-9.util :as u]))

(defn to-buffergeometry
  "Converts a thi.ng GLMesh into a THREE.BufferGeometry."
  [mesh]
  (let [buffer-geom (THREE.BufferGeometry.)]
    (.addAttribute buffer-geom "position"
                   (THREE.BufferAttribute. (:vertices mesh) 3))
    (.addAttribute buffer-geom "color"
                   (THREE.BufferAttribute. (:cols mesh) 4))
    buffer-geom))

; FIXME: Temporary, for three-mesh below, shouldn't be in this namespace at all
(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

(defn three-mesh
  "Builds a Three.js mesh object out of the given mesh."
  [mesh]
  ; FIXME: Implement properly.
  (let [material-properties #js {;:color 0xf21d6b
                                 :shading js/THREE.FlatShading
                                 :vertexColors js/THREE.VertexColors
                                 :side js/THREE.DoubleSide}
        geometry (to-buffergeometry mesh)
        material (THREE.MeshBasicMaterial. material-properties)
        mesh (THREE.Mesh. geometry material)
        position (random-vector 1.0)]
    ;; setting .-position directly doesn't work, you have to go component by
    ;; component... how lame
    (set! (.-x (.-position mesh)) (.-x position))
    (set! (.-y (.-position mesh)) (.-y position))
    (set! (.-z (.-position mesh)) (.-z position))
    mesh))

(defn debug->mesh [d]
  (let [material-properties #js {:color (-> d :color c/as-int32 deref)}
        material (THREE.LineBasicMaterial. material-properties)
        geometry (THREE.BufferGeometry.)
        pos (THREE.BufferAttribute.
             (ta/float32 (mapcat #(vector (:x %) (:y %) (:z %)) (:geom d))) 3)
        line (THREE.Line. geometry material)]
    (.addAttribute geometry "position" pos)
    line))
