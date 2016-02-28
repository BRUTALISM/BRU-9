(ns bru-9.geom.mesh
  (:require [thi.ng.geom.core.vector :as v]
            [thi.ng.geom.basicmesh :as bm]
            [bru-9.util :as u]))

(def empty-mesh (bm/basic-mesh))

(defn make-mesh
  "Makes a thi.ng mesh out of the given collection of vertices (verts) and
  collection of triples representing face indices (idxs)"
  ; TODO: Add more arities (colors, etc)
  [verts idxs]
  (reduce #(bm/add-face %1 (u/nths verts %2)) empty-mesh idxs))

(defn merge-meshes
  "Reducer function which takes two thi.ng meshes and merges them into one"
  ; TODO: Support for colors
  [m1 m2]
  (reduce bm/add-face m1 (:faces m2)))

(def tetrahedron
  (make-mesh [(v/vec3 -1 0 -1)
              (v/vec3 0 0 1)
              (v/vec3 1 0 -1)
              (v/vec3 0 2 0)]
             [[0 2 1]
              [0 1 3]
              [1 2 3]
              [0 3 2]]))
