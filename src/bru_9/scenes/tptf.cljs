(ns bru-9.scenes.tptf
  (:require [bru-9.util :as u]
            [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.interop :as i]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.circle :as circle]
            [thi.ng.geom.webgl.glmesh :as glm]))

(def config {:elements 200})

(defn element [acc color]
  (let []
    (-> (take 3 (repeatedly #(v/randvec3 (rand 10))))
        (ptf/sweep-mesh
         (g/vertices (circle/circle 0.1) 5)
         {:mesh acc
          :attribs {:col (-> color (repeat) (attr/const-face-attribs))}}))))

;; Scene setup & main loop

(defonce *state* (atom {}))

(defn setup [initial-context]
  (let [scene (:scene initial-context)
        acc (glm/gl-mesh 65536 #{:col})
        palette (c/random-palette)
        palette (ci/infinite-palette palette {:hue 0.3 :saturation 0.2})
        elem-count (:elements config)
        mesh (reduce element acc (take elem-count palette))
        three-mesh (i/three-mesh mesh)]
    (.add scene three-mesh)
    (reset! *state* (assoc @*state* :scene scene))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
