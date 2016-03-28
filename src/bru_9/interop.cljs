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

(defn three-mesh
  "Builds a Three.js mesh object out of the given mesh."
  [mesh]
  (let [material-properties #js {;:color 0xf21d6b
                                 :shading js/THREE.FlatShading
                                 :vertexColors js/THREE.VertexColors
                                 :side js/THREE.DoubleSide}
        geometry (to-buffergeometry mesh)
        material (THREE.MeshBasicMaterial. material-properties)]
    (THREE.Mesh. geometry material)))

(defn debug->mesh [d]
  (let [material-properties #js {:color (-> d :color c/as-int32 deref)}
        material (THREE.LineBasicMaterial. material-properties)
        geometry (THREE.BufferGeometry.)
        pos (THREE.BufferAttribute.
             (ta/float32 (mapcat #(vector (:x %) (:y %) (:z %)) (:geom d))) 3)
        line (THREE.Line. geometry material)]
    (.addAttribute geometry "position" pos)
    line))
