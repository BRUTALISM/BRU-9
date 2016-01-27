(ns bru-9.geom.mesh)

; TODO: Define a mesh data structure, or just use thi.ng's Mesh

(defn build-mesh
  "Builds a mesh out of the given collection of partial meshes"
  [ps]
  (let []
    ; TODO: Implement.
    ))

; FIXME: Temporary, for three-mesh below, shouldn't be in this namespace at all
(defn random-vector [extent]
  (let [r (fn [range] (- (* range (.random js/Math)) (/ range 2)))]
    (THREE.Vector3. (r extent) (r extent) (r extent))))

(defn three-mesh
  "Builds a Three.js mesh object out of the given mesh."
  [m]
  ; FIXME: Implement properly.
  (let [material-properties #js {:color 0xAAAAAA
                                 :shading js/THREE.FlatShading}
        geometry (THREE.BoxGeometry. 0.01 0.01 0.2)
        material (THREE.MeshBasicMaterial. material-properties)
        mesh (THREE.Mesh. geometry material)
        position (random-vector 1.5)]
    ;; setting .-position directly doesn't work, you have to go component by
    ;; component... how lame
    (set! (.-x (.-position mesh)) (.-x position))
    (set! (.-y (.-position mesh)) (.-y position))
    (set! (.-z (.-position mesh)) (.-z position))
    mesh))
