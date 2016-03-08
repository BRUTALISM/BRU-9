(ns bru-9.geom.mesh
  (:require [thi.ng.geom.core.vector :as v]
            [thi.ng.geom.gmesh :as gm]
            [thi.ng.color.core :as c]
            [bru-9.util :as u]))

(def empty-mesh (gm/gmesh))

(defn make-mesh
  "Makes a thi.ng mesh out of the given collection of vertices (verts),
  collection of triples representing face indices (idxs), and collection of
  colors for each vertex."
  ([verts idxs]
   (reduce #(gm/add-face %1 (u/nths verts %2)) empty-mesh idxs))
  ([verts idxs colors]
   (let [mesh (make-mesh verts idxs)
         color-attrib {:colors colors}
         built-mesh (assoc mesh :attribs color-attrib)]
     built-mesh)))

(defn merge-attribs
  "Merges app-specific custom attributes of the two given meshes."
  [m1 m2]
  (let [color-path [:attribs :colors]
        colors1 (get-in m1 color-path [])]
;;     (prn (str "COLORS: " (get-in m2 color-path)))
    (update-in m1 color-path #(into (or % []) (get-in m2 color-path)))))

(defn merge-meshes
  "Reducer function which takes two thi.ng meshes and merges them into one."
  [m1 m2]
  (let [mesh (reduce gm/add-face m1 (:faces m2))]
;;     (prn (str "m1: " (:attribs m1)))
;;     (prn (str "m2: " (:attribs m2)))
;;     (prn (str "mesh: " (:attribs mesh)))
    (merge-attribs mesh m2)))

(def tetrahedron
  (let [verts [(v/vec3 -1 0 -1)
               (v/vec3 0 0 1)
               (v/vec3 1 0 -1)
               (v/vec3 0 2 0)]
        indices [[0 2 1]
                 [0 1 3]
                 [1 2 3]
                 [0 3 2]]
        colors [(c/random-rgb)
                (c/random-rgb)
                (c/random-rgb)
                (c/random-rgb)]]
    (make-mesh verts indices colors)))
