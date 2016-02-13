(ns bru-9.geom.mesh)

; TODO: Define a mesh data structure, or just use thi.ng's Mesh
; {:vertices [v1 v2 v3 ...]
;  :faces [[0 2 1] [3 2 1] ...]
;  :colors [c1 c2 c3 ...]}

(def empty-mesh
  {:vertices []
   :faces []
   :colors []})

(defn merge-meshes
  "Reducer function which takes two meshes and merges them into one"
  [m1 m2]
  {:vertices (apply conj (:vertices m1) (:vertices m2))
   :faces (apply conj (:faces m1) (map #(+ (count (:faces m1)))
                                       (:faces m2)))
   :colors (apply conj (:colors m1) (:colors m2))})

; FIXME: Temporary, for three-mesh below, shouldn't be in this namespace at all
(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

(defn three-mesh
  "Builds a Three.js mesh object out of the given mesh."
  [m]
  ; FIXME: Implement properly.
  (let [material-properties #js {:color 0xf21d6b
                                 :shading js/THREE.FlatShading}
        geometry (THREE.BoxGeometry. 0.01 0.01 0.2)
        material (THREE.MeshBasicMaterial. material-properties)
        mesh (THREE.Mesh. geometry material)
        position (random-vector 1.0)]
    ;; setting .-position directly doesn't work, you have to go component by
    ;; component... how lame
    (set! (.-x (.-position mesh)) (.-x position))
    (set! (.-y (.-position mesh)) (.-y position))
    (set! (.-z (.-position mesh)) (.-z position))
    mesh))
