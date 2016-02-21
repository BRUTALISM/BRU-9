;; thi.ng -> THREE.js interop functions

(ns bru-9.interop)

(defn to-vector3
  "Converts a Vector3 into a THREE.Vector3"
  [v3]
  (THREE.Vector3. (:x v3) (:y v3) (:z v3)))

(defn to-face3
  "Converts a given triple into a THREE.Face3"
  [t]
  ; TODO: convert from thi.ng face
  (let [[v1 v2 v3] t]
    (THREE.Face3. v1 v2 v3)))

(defn to-color
  "Converts a given color (in integer hex format) into a THREE.Color"
  [c]
  (THREE.Color. c))

(defn to-geometry
  "Creates a Three.js Geometry out of the given mesh"
  [mesh]
  ; TODO: convert from thi.ng mesh
  (let [vertices (map to-vector3 (:vertices mesh))
        faces (map to-face3 (:faces mesh))
        colors (map to-color (:colors mesh))
        geometry (THREE.Geometry.)]
    (doseq [v vertices] (.push (.-vertices geometry) v))
    (doseq [f faces] (.push (.-faces geometry) f))
    (doseq [c colors] (.push (.-colors geometry) c))
    geometry))

; FIXME: Temporary, for three-mesh below, shouldn't be in this namespace at all
(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

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
