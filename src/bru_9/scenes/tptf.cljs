(ns bru-9.scenes.tptf
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.interop :as i]
            [bru-9.util :as u]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.circle :as circle]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.math.core :as m]))

(def config {:elements 20
             :walk-length 32
             :walk-multiplier 1.2
             :circle-resolution 16
             :field-dimension 20})

(defonce field
  (let [dim (:field-dimension config)]
    (fl/linear-field [dim dim dim] #(v/randvec3))))

(defn random-offsets [n m]
  (take n (repeatedly #(v/randvec3 m))))

(defonce offsets (random-offsets (:walk-length config) 0.2))

(defn element [acc color]
  (let [hops (:walk-length config)
        cr (:circle-resolution config)
        mul (:walk-multiplier config)
        hres (/ (:field-dimension config) 2)]
    (-> (map #(m/+ %1 %2)
             (f/walk field (m/+ (v/vec3 hres hres hres) (v/randvec3)) hops mul)
             offsets)
        (ptf/sweep-mesh
         (g/vertices (circle/circle 0.05) cr)
         {:mesh acc
          :attribs {:col (-> color (repeat) (attr/const-face-attribs))}}))))

;; Scene setup & main loop

(defn setup [initial-context]
  (let [scene (:scene initial-context)
        acc (glm/gl-mesh 65536 #{:col})
        palette (c/random-palette)
        palette (ci/infinite-palette palette {:hue 0.3 :saturation 0.2})
        elem-count (:elements config)
        mesh (reduce element acc (take elem-count palette))
        three-mesh (i/three-mesh mesh)]
    (.add scene three-mesh)
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
