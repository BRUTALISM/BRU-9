(ns bru-9.scenes.tbezier
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.geom.bezier :as b]
            [bru-9.util :as u]
            [bru-9.geom.ptf :as ptf]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.geom.circle :as cir]
            [thi.ng.geom.attribs :as attr]
            [bru-9.color.infinite :as ci]
            [bru-9.interop :as i]))

(def config {:background-color 0xEEEEEE
             :start-positions-hops 30
             :start-positions-axis-following 2.0
             :start-positions-walk-multiplier 0.02
             :curve-tightness 0.08
             :spline-hops 6
             :offset-radius 0.2
             :field-dimensions [5 5 5]
             :field-count 3
             :field-general-direction v/V3X
             :field-random-following 1.2
             :mulfn-base 0.6
             :mulfn-jump-chance 0.1
             :mulfn-jump-intensity 1.5
             :wander-probability 0.2})

(defn- mulfn [_]
  (let [{:keys [mulfn-base mulfn-jump-chance mulfn-jump-intensity]} config]
    (+ mulfn-base (if (< (rand) mulfn-jump-chance) mulfn-jump-intensity 0))))

(defn- field-generator [_ random-intensity direction]
  (m/+ direction (v/randvec3 random-intensity)))

(defn- make-directions [initial count]
  (loop [dirs [initial], i count]
    (if (> i 0)
      (recur (conj dirs (m/- (last dirs))) (dec i))
      (rest dirs))))

(defn- make-fields []
  (let [{:keys [field-count
                field-general-direction
                field-random-following
                field-dimensions]} config
        dirs (make-directions field-general-direction field-count)
        fgen (fn [_ dir] (field-generator _ field-random-following dir))
        constructor (fn [dir] (fl/linear-field field-dimensions #(fgen % dir)))]
    (map constructor dirs)))

(defn- make-start-positions-field [direction]
  (let [random-follow (:field-random-following config)]
    (fl/linear-field (:field-dimensions config)
                     #(field-generator % random-follow direction))))

(defn- make-start-positions [field]
  (let [mulfn (fn [_] (:start-positions-walk-multiplier config))]
    (f/walk field v/V3 (:start-positions-hops config) mulfn)))

(def start-positions
  (->> (:start-positions-axis-following config)
       (m/* (:field-general-direction config))
       make-start-positions-field
       make-start-positions))

(defn- make-field-splines
  "Returns a collection of splines generated by walking the given fields."
  [fields]
  (let [{:keys [spline-hops
                offset-radius
                curve-tightness
                wander-probability]} config
        offset-positions (map #(m/+ % (v/randvec3 offset-radius))
                              start-positions)]
    (map #(b/spline-wander fields % spline-hops mulfn curve-tightness
                           wander-probability)
         offset-positions)))

(def circles
  (let [count 100
        max-radius 0.1
        circle-vertices 6
        mfn
        (fn [i]
          (let [t (/ i (dec count))]
            (g/vertices (cir/circle (* max-radius (u/sin (* t m/PI))))
                        circle-vertices)))]
    (map mfn (range count))))

(defn- draw-fields
  "Draws the given field using the given infinite palette."
  [scene fields palette]
  (let [mesh-acc (glm/gl-mesh 65536 #{:col})
        generate-profiles
        (fn [count]
          (let [mfn
                (fn [i]
                  (let [t (/ i (dec count))]
                    (u/nth01 circles t)))]
            (map mfn (range count))))
        ptf-spline
        (fn [acc spline color]
          (let [colors (attr/const-face-attribs (repeat color))
                vertices (g/vertices spline)]
            (ptf/sweep-mesh vertices
                            (generate-profiles (count vertices))
                            {:mesh acc, :attribs {:col colors}})))
        mesh (reduce #(ptf-spline %1 (first %2) (second %2))
                     mesh-acc
                     (map vector (make-field-splines fields) palette))
        three-mesh (i/three-mesh mesh)]
    (.add scene three-mesh)))

(defn- setup-camera [camera]
  (set! (.-x (.-position camera)) 2)
  (set! (.-y (.-position camera)) 0)
  (set! (.-z (.-position camera)) 30)
  (.lookAt camera (THREE.Vector3. 2 0 0)))

(defn setup [initial-context]
  (draw-fields (:scene initial-context)
               (make-fields)
               (ci/infinite-palette (c/random-palette)))
  (setup-camera (:camera initial-context))
  (.setClearColor (:renderer initial-context) (:background-color config) 1.0)
  initial-context)

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))
