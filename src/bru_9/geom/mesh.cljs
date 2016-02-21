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
  ; TODO: convert to thi.ng mesh merging - BasicMesh constructor already handles
  ; multiple meshes as arguments
  {:vertices (apply conj (:vertices m1) (:vertices m2))
   :faces (apply conj (:faces m1) (map #(mapv (partial + (count (:faces m1))) %)
                                       (:faces m2)))
   :colors (apply conj (:colors m1) (:colors m2))})
