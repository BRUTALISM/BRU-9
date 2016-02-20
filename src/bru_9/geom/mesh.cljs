(ns bru-9.geom.mesh
  (:require [bru-9.geom.vector3 :as v]))


;; {:vertices [v1 v2 v3 ...]
;;  :faces [[0 2 1] [3 2 1] ...]
;;  :colors [c1 c2 c3 ...]}

(def empty-mesh
  {:vertices []
   :faces []
   :colors []})

(def tetrahedron
  {:vertices [(v/Vector3. -1 0 -1)
              (v/Vector3. 0 0 1)
              (v/Vector3. 1 0 -1)
              (v/Vector3. 0 2 0)]
   :faces [[0 2 1]
           [0 1 3]
           [1 2 3]
           [0 3 2]]})

(defn merge-meshes
  "Reducer function which takes two meshes and merges them into one"
  [m1 m2]
  {:vertices (apply conj (:vertices m1) (:vertices m2))
   :faces (apply conj (:faces m1) (map #(mapv (partial + (count (:faces m1))) %)
                                       (:faces m2)))
   :colors (apply conj (:colors m1) (:colors m2))})

; FIXME: Temporary, for three-mesh below, shouldn't be in this namespace at all
(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

(defn to-face3
  "Converts a given triple into a THREE.Face3"
  [t]
  (let [[v1 v2 v3] t]
    (THREE.Face3. v1 v2 v3)))

(defn to-color
  "Converts a given color (in integer hex format) into a THREE.Color"
  [c]
  (THREE.Color. c))

(defn to-geometry
  "Creates a Three.js Geometry out of the given mesh"
  [mesh]
  (let [vertices (map v/to-vector3 (:vertices mesh))
        faces (map to-face3 (:faces mesh))
        colors (map to-color (:colors mesh))
        geometry (THREE.Geometry.)]
    (doseq [v vertices] (.push (.-vertices geometry) v))
    (doseq [f faces] (.push (.-faces geometry) f))
    (doseq [c colors] (.push (.-colors geometry) c))
    geometry))

(defn three-mesh
  "Builds a Three.js mesh object out of the given mesh"
  [mesh]
  ; FIXME: Implement properly.
  (let [material-properties #js {:color 0xf21d6b
                                 :shading js/THREE.FlatShading}
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
