(ns bru-9.geom.vector3)

(defrecord Vector3 [x y z])

(defn to-vector3
  "Converts a Vector3 into a THREE.Vector3"
  [v3]
  (THREE.Vector3. (:x v3) (:y v3) (:z v3)))
