(ns bru-9.scenes.tbezier
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.debug :as debug]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.bezier :as b]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]))

(def config {:splines-per-field 200
             :num-hops 10
             :offset-radius 0.2
             :field-dimensions [5 5 5]
             :field-directions [v/V3Z v/V3Y] ;;[(v/randvec3) (v/randvec3)]
             })

(defn- field-generator [coords intensity direction]
  (m/+ direction (v/randvec3 intensity)))

(defn- make-field [direction]
  (fl/linear-field (:field-dimensions config)
                   #(field-generator % 1.3 direction)))

(defn- spline-walk
  "Returns a bezier spline resulting from walking a given field from the given
  start position, using the given number of hops."
  [field startpos hops]
  (b/auto-spline3 (f/walk field startpos hops (+ 0.3 (rand 0.1)))))

(defn- make-field-splines
  "Returns a collection of splines generated by walking the given field."
  [field]
  (let [{:keys [splines-per-field num-hops offset-radius]} config
        zdim (last (f/dimensions field))
        zcenters (range 0 zdim (/ zdim splines-per-field))
        centers (map #(m/+ (v/vec3 0 0 %) (v/randvec3 offset-radius)) zcenters)]
    (map #(spline-walk field % num-hops) centers)))

(defn- draw-field
  "Draws the given field using the given infinite palette."
  [field palette]
  (doseq [[spline color] (map vector (make-field-splines field) palette)]
      (debug/line-strip (g/vertices spline) color)))

(defn- setup-camera [camera]
  (set! (.-x (.-position camera)) 0)
  (set! (.-y (.-position camera)) -14)
  (set! (.-z (.-position camera)) 4)
  (.lookAt camera (THREE.Vector3. 0 0 4)))

(defn setup [initial-context]
  (let [fields (map make-field (:field-directions config))]
    (doseq [field fields]
      (draw-field field (ci/infinite-palette (c/random-palette))))
    (setup-camera (:camera initial-context))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
