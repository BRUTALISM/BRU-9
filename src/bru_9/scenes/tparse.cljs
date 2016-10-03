(ns bru-9.scenes.tparse
  (:require [bru-9.requests :as req]
            [bru-9.parse :as parse]
            [bru-9.geom.tag :as gtag]
            [bru-9.interop :as i]
            [bru-9.util :as u]
            [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.geom.vector :as v]
            [bru-9.geom.generators :as gen]
            [thi.ng.math.core :as m]
            [thi.ng.color.core :as tc]))

(def config {:url "http://pitchfork.com"
             :url-regex "http(s)?://(\\w|-)+\\.((\\w|-)+\\.?)+"
             :all-seeing ["facebook" "google" "instagram" "twitter" "amazon"]
             :node-limit 400
             :camera-distance 14
             :background-color 0x111111
             :start-positions-axis-following 1.7
             ; TODO: calculate walk multiplier based on number of nodes
             :start-positions-walk-multiplier 0.01
             :curve-tightness-min 0.04
             :curve-tightness-max 0.1
             :spline-hops 4
             :offset-radius 0.5
             :field-dimensions [10 5 5]
             :field-count 2
             :field-general-direction v/V3X
             :field-random-following 1.3
             :mulfn-base 0.8
             :mulfn-jump-chance 0.2
             :mulfn-jump-intensity 1.0
             :wander-probability 0.25
             :spline-resolution 10
             :mesh-geometry-size 131070
             :infinite-params {:hue 0.1
                               :saturation 0.2
                               :brightness -0.1}
             :rotation-speed 0.0002})

(defonce *state* (atom {}))

; Geometry generation

(defn- make-fields []
  (let [{:keys [field-count
                field-general-direction
                field-random-following
                field-dimensions]} config]
    (gen/make-fields field-count
                     field-dimensions
                     field-general-direction
                     field-random-following)))

(defn make-start-positions [count]
  (let [{:keys [start-positions-walk-multiplier
                start-positions-axis-following
                field-general-direction
                field-random-following
                field-dimensions]} config
        mulfn (fn [_] start-positions-walk-multiplier)
        direction (m/* field-general-direction start-positions-axis-following)
        start-positions-field
        (gen/make-start-positions-field field-dimensions
                                        direction
                                        field-random-following)]
    (gen/make-start-positions start-positions-field count mulfn)))

(defn- mulfn [_]
  (let [{:keys [mulfn-base mulfn-jump-chance mulfn-jump-intensity]} config]
    (+ mulfn-base (if (< (rand) mulfn-jump-chance) mulfn-jump-intensity 0))))

(defn- make-palette []
  (let [palette (c/random-palette)
        infinite (ci/infinite-palette palette (:infinite-params config))]
    infinite))

(declare setup-pivot)

(defn nodes->mesh [nodes]
  (let [acc (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        fields (make-fields)
        start-positions (make-start-positions (count nodes))
        splines (gen/make-field-splines fields start-positions mulfn config)
        palette (make-palette)
        ;palette (repeatedly tc/random-rgb)
        nodes-splines-colors (map vector nodes splines palette)
        tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline color
                                (:spline-resolution config)))
        mesh (reduce tagfn acc nodes-splines-colors)]
    (swap! *state* assoc :splines splines)
    (setup-pivot)
    mesh))

; URL parsing

(defn process-response
  "Parses the given response, converts its DOM tree into a mesh, and adds the
  mesh to the current Three.js scene"
  [response]
  (let [body (:body response)
        seers (parse/map-occurences body (:all-seeing config))
        urls (set (parse/occurences body (:url-regex config)))
        all-nodes (parse/level-dom body)
        supported-tags (gtag/all-tags)
        filtered-nodes (filter #(get supported-tags (first %)) all-nodes)
        limited-nodes (take (:node-limit config) filtered-nodes)
        mesh (nodes->mesh limited-nodes)
        three-mesh (i/three-mesh mesh)
        scene (:scene @*state*)]
    (println "Parsed nodes: " (count all-nodes))
    (println "URLs: " urls)
    (println "URL count: " (count urls))
    (println "Seers: " seers)
    (.add scene three-mesh)))

; Scene setup & main loop

(defn- setup-camera [camera pivot-pos]
  (set! (.-x (.-position camera)) (.-x pivot-pos))
  (set! (.-y (.-position camera)) 0)
  (set! (.-z (.-position camera)) (:camera-distance config))
  (.lookAt camera pivot-pos))

(defonce camera-pivot (THREE.Object3D.))

(defn- setup-pivot []
  (let [{:keys [splines camera scene]} @*state*
        [xmin xmax] (u/calculate-x-extents splines)
        pivot-pos (THREE.Vector3. (:x (m/div (m/+ xmax xmin) 2)) 0 0)]
    (setup-camera camera pivot-pos)
    (set! (.-position camera-pivot) pivot-pos)
    (.add camera-pivot camera)
    (.add scene camera-pivot)))

(defn- on-reload [context]
  (let [{:keys [renderer]} context
        {:keys [background-color]} config]
    (.setClearColor renderer background-color 1.0)
    (req/get-url (:url config) process-response)
    context))

(defn setup [initial-context]
  (swap! *state* assoc :scene (:scene initial-context))
  (swap! *state* assoc :camera (:camera initial-context))
  (on-reload initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (.remove camera-pivot (:camera context))
    (on-reload context)))

(defn animate [context]
  (let [rot (.-x (.-rotation camera-pivot))]
    (set! (.-x (.-rotation camera-pivot)) (+ rot (:rotation-speed config)))
    context))
