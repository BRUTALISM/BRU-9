(ns bru-9.scenes.throwups
  (:require [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.geom.bezier :as b]
            [bru-9.geom.brush :as br]
            [bru-9.util :as u]
            [bru-9.geom.ptf :as ptf]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.geom.attribs :as attr]
            [bru-9.color.infinite :as ci]
            [bru-9.interop :as i]
            [thi.ng.color.core :as tc]
            [bru-9.geom.generators :as gen]))

(defn sample-brush [brushfn]
  (let [sample-count 100
        bfn (fn [i] (brushfn (/ i (dec sample-count))))]
    (map bfn (range sample-count))))

(def config {:background-color 0x000000
             :start-positions-hops 200
             :start-positions-axis-following 1.7
             :start-positions-walk-multiplier 0.03
             :curve-tightness-min 0.04
             :curve-tightness-max 0.1
             :spline-hops 5
             :offset-radius 0.05
             :field-dimensions [10 5 5]
             :field-count 2
             :field-general-direction v/V3X
             :field-random-following 1.0
             :field-noise 1.6
             :mulfn-base 0.7
             :mulfn-jump-chance 0.3
             :mulfn-jump-intensity 1.2
             :wander-probability 0.25
             :spline-resolution 10
             :mesh-geometry-size 131070
             :brushes [
                       ;(sample-brush #(br/sine % 0.005 m/PI 3 2))
                       ;(sample-brush #(br/sine % 0.009 m/PI 3 2))
                       ;(sample-brush #(br/sine % 0.011 m/PI 3 2))
                       ;(sample-brush #(br/sine % 0.015 m/PI 3 2))

                       (sample-brush #(br/sine % 0.08 m/PI 4 2))
                       (sample-brush #(br/sine % 0.16 m/PI 4 2))
                       (sample-brush #(br/sine % 0.18 m/PI 4 2))

                       ;(sample-brush #(br/wobbler % 0.24))

                       ;(sample-brush #(br/two-sided-spikes % 0.4 5))
                       ;(sample-brush #(br/two-sided-spikes % 0.24 5))
                       ;(sample-brush #(br/two-sided-spikes % 0.12 5))
                       ;(sample-brush #(br/two-sided-spikes % 0.06 5))

                       ;(sample-brush #(br/sine % 0.5 (rand m/TWO_PI) 5 1))
                       ;(sample-brush #(br/rotating-quad % 0.04 m/HALF_PI))
                       ;(sample-brush #(br/rotating-quad % 0.08 m/HALF_PI))

                       ;(sample-brush #(br/noise-quad % 0.15))
                       ;(sample-brush #(br/noise-quad % 0.1))
                       ;(sample-brush #(br/noise-quad % 0.05))
                       ]
             :infinite-params {:hue 0.1
                               :saturation 0.2
                               :brightness 0.0}
             :rotation-speed 0.00015
             :camera-distance 12})

; Generator-specific definitions

(def start-positions
  (let [mulfn (fn [_] (:start-positions-walk-multiplier config))
        hops (:start-positions-hops config)
        axis-follow (:start-positions-axis-following config)
        direction (m/* (:field-general-direction config) axis-follow)
        random-intensity (:field-random-following config)
        dimensions (:field-dimensions config)
        start-positions-field (gen/make-start-positions-field dimensions
                                                              direction
                                                              random-intensity)]
    (gen/make-start-positions start-positions-field hops mulfn)))

(defn- mulfn [_]
  (let [{:keys [mulfn-base mulfn-jump-chance mulfn-jump-intensity]} config]
    (+ mulfn-base (if (< (rand) mulfn-jump-chance) mulfn-jump-intensity 0))))

; Drawing logic

(defn- draw-fields
  "Draws the given field using the given infinite palette. Returns the splines."
  [scene fields palette]
  (let [mesh-acc (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        res (:spline-resolution config)
        brushes (:brushes config)
        splines (gen/make-field-splines fields start-positions mulfn config)
        generate-profiles
        (fn [count brush]
          (let [mfn
                (fn [i]
                  (let [t (/ i (dec count))]
                    (u/nth01 brush t)))]
            (map mfn (range count))))
        ptf-spline
        (fn [acc spline color]
          (let [colors (attr/const-face-attribs
                         (repeatedly #(tc/random-analog color 0.3)))
                vertices (g/vertices spline res)
                brush (rand-nth brushes)]
            (ptf/sweep-mesh vertices
                            (generate-profiles (count vertices) brush)
                            {:mesh acc, :attribs {:col colors}})))
        mesh (reduce #(ptf-spline %1 (first %2) (second %2))
                     mesh-acc
                     (map vector splines palette))
        three-mesh (i/three-mesh mesh)]
    (.add scene three-mesh)
    splines))

; Scene-related stuff

(defn- setup-camera [camera pivot-pos]
  (set! (.-x (.-position camera)) (.-x pivot-pos))
  (set! (.-y (.-position camera)) 0)
  (set! (.-z (.-position camera)) (:camera-distance config))
  (.lookAt camera pivot-pos))

(defonce camera-pivot (THREE.Object3D.))

(defn setup [initial-context]
  (let [camera (:camera initial-context)
        scene (:scene initial-context)
        {:keys [field-count
                field-general-direction
                field-random-following
                field-dimensions
                field-noise
                background-color
                infinite-params]} config
        infinite (ci/infinite-palette [(tc/random-rgb)] infinite-params)
        fields (gen/make-fields field-count
                                field-dimensions
                                field-general-direction
                                field-random-following
                                field-noise)
        splines (draw-fields (:scene initial-context) fields infinite)
        [xmin xmax] (u/calculate-x-extents splines)
        pivot-pos (THREE.Vector3. (:x (m/div (m/+ xmax xmin) 2)) 0 0)]
    (setup-camera camera pivot-pos)
    (set! (.-position camera-pivot) pivot-pos)
    (.add camera-pivot camera)
    (.add scene camera-pivot)

    (.setClearColor (:renderer initial-context) background-color 1.0)
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (.remove camera-pivot (:camera context))
    (setup context)))

(defn animate [context]
  (let [rot (.-x (.-rotation camera-pivot))]
    (set! (.-x (.-rotation camera-pivot)) (+ rot (:rotation-speed config)))
    context))
