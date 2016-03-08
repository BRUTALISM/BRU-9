;; thi.ng -> THREE.js interop functions

(ns bru-9.interop
  (:require [thi.ng.geom.core :as g]
            [thi.ng.typedarrays.core :as ta]
            [thi.ng.color.core :as c]
            [bru-9.util :as u]))

(defn to-vector3
  "Converts a Vector3 into a THREE.Vector3."
  [v3]
  (THREE.Vector3. (:x v3) (:y v3) (:z v3)))

(defn to-face3
  "Converts a given triple into a THREE.Face3."
  [t]
  (let [[v1 v2 v3] t]
    (THREE.Face3. v1 v2 v3)))

(defn face-indices
  "Returns a collection of triples representing mesh faces as triangle indices
  w/ regard to the given sequence of vertices. (thi.ng's mesh vertices are in a
  hash map, so an ordered vertex sequence needs to be supplied in order for
  indexes to have any meaning.)"
  [mesh vertices]
  (let [faces (g/faces mesh)
        face-indices (fn [face] (map #(u/first-index vertices %) face))]
    (map face-indices faces)))

(defn add-colors
  "Adds the given vertex color array to the given THREE.Geometry, and returns a
  THREE.BufferGeometry object. The colors are assumed to be thi.ng colors."
  [geom colors]
  (let [buffer-geom (THREE.BufferGeometry.)
        components [c/red c/green c/blue]
        get-components (fn [c] (map #(% c) components))
        mapped-colors (mapcat get-components colors)
        color-buf (ta/float32 mapped-colors)]
;;     (prn (str "MAPPED COLORS: " mapped-colors))
;;     (prn (str "GEOMETRY: " geom))
    (.fromGeometry buffer-geom geom)
    (.addAttribute buffer-geom "color"
                   (THREE.BufferAttribute. color-buf (count components)))
    buffer-geom))

(defn to-geometry
  "Creates a Three.js Geometry out of the given mesh."
  [mesh]
  (let [vs (into [] (g/vertices mesh))
        vertices (map to-vector3 vs)
        faces (map to-face3 (face-indices mesh vs))
        colors (get-in mesh [:attribs :colors])
        geometry (THREE.Geometry.)]
;;     (prn (str (count vertices) " =? " (count colors)))
    (doseq [v vertices] (.push (.-vertices geometry) v))
    (doseq [f faces] (.push (.-faces geometry) f))
    (if colors
      (add-colors geometry colors)
      geometry)))

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
                                 :vertexColors js/THREE.VertexColors}
        geometry (to-geometry mesh)
        material (THREE.MeshBasicMaterial. material-properties)
        mesh (THREE.Mesh. geometry material)
        position (random-vector 1.0)]
    ;; setting .-position directly doesn't work, you have to go component by
    ;; component... how lame
    (set! (.-x (.-position mesh)) (.-x position))
    (set! (.-y (.-position mesh)) (.-y position))
    (set! (.-z (.-position mesh)) (.-z position))
    mesh))
