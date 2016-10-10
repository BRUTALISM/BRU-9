(ns bru-9.scenes.tparse
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [<! >!]]
            [bru-9.requests :as req]
            [bru-9.parse :as parse]
            [bru-9.geom.tag :as gtag]
            [bru-9.interop :as i]
            [bru-9.util :as u]
            [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [bru-9.geom.generators :as gen]
            [bru-9.geom.bezier :as b]
            [bru-9.geom.ptf :as ptf]
            [bru-9.field.linear :as fl]
            [bru-9.debug :as d]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]
            [thi.ng.color.core :as tc]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.sphere :as sphere]
            [thi.ng.geom.attribs :as attr]))

(def config {:url-regex "http(s)?://(\\w|-)+\\.((\\w|-)+\\.?)+"
             ;:url "http://polumenta.zardina.org"
             :url "http://brutalism.rs/category/process/"
             ;:url "http://apple.com"
             ;:url "http://pitchfork.com"
             ;:url "http://nytimes.com"
             ;:url "http://slashdot.org"
             :all-seeing ["facebook" "google" "instagram" "twitter" "amazon"]
             :node-limit 5000
             :nodes-per-batch 100
             :camera-distance 16
             :background-color 0x111111
             :start-positions-axis-following 1.5
             :start-positions-walk-multiplier 0.015
             :start-positions-random-offset 0.5
             :background-points-per-spline 6
             :background-spline-tightness 0.05
             :background-spline-random-offset 0.1
             :external-radius-min 1.0
             :external-radius-max 3.0
             :external-angle-min m/SIXTH_PI
             :external-angle-max m/HALF_PI
             :external-tightness-min 0.1
             :external-tightness-max 0.3
             :external-x-wobble 1.2
             :external-node-count 5
             :curve-tightness-min 0.04
             :curve-tightness-max 0.1
             :spline-hops 4
             :field-dimensions [10 5 5]
             :field-count 2
             :field-general-direction v/V3X
             :field-random-following 1.6
             :mulfn-base 0.8
             :mulfn-jump-chance 0.1
             :mulfn-jump-intensity 1.0
             :wander-probability 0.25
             :spline-resolution 10
             :mesh-geometry-size 65535
             :infinite-params {:hue 0.1
                               :saturation 0.1
                               :brightness 0.1}
             :rotation-speed 0.0001
             })

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
        ;palette [(-> (tc/random-rgb) (tc/adjust-saturation 1.0))]
        infinite (ci/infinite-palette palette (:infinite-params config))]
    infinite))

(defn spline-nodes->mesh [{:keys [nodes splines palette]}]
  (let [gl-mesh (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        nodes-splines-colors (map vector nodes splines palette)
        tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline color
                                (:spline-resolution config)))]
    (async/put! (:mesh-chan @*state*)
                (reduce tagfn gl-mesh nodes-splines-colors))))

; Spline creation

(defn make-background-splines [start-positions num]
  (let [pps (:background-points-per-spline config)
        group-size (int (u/floor (/ (count start-positions) pps)))
        random-offset (:background-spline-random-offset config)
        tuples (->> start-positions
                    (map #(m/+ % (v/randvec3 random-offset)))
                    (partition group-size)
                    (map shuffle)
                    (apply interleave)
                    (partition pps))
        tightness (:background-spline-tightness config)]
    (map #(b/auto-spline3 % tightness) (take num tuples))))

(defn make-external-splines [start-positions num]
  (let [rmin (:external-radius-min config)
        rmax (:external-radius-max config)
        amin (:external-angle-min config)
        amax (:external-angle-max config)
        tmin (:external-tightness-min config)
        tmax (:external-tightness-max config)
        xw (:external-x-wobble config)
        node-count (:external-node-count config)
        angling (fn [a] (+ a (u/rand-range amin amax)))
        make-node
        (fn [p a rmax]
          (let [r (u/rand-range rmin rmax)
                v (v/vec3 (u/rand-range (- xw) xw) 0 r)]
            (m/+ p (g/rotate-around-axis v v/V3X a))))
        make-spline-vertices
        (fn [start-pos]
          (map make-node
               (repeat node-count start-pos)
               (iterate angling (rand m/TWO_PI))
               (repeatedly #(u/rand-range rmin rmax))))
        make-spline
        (fn [vertices]
          (b/auto-spline3 vertices (u/rand-range tmin tmax)))
        start-positions (repeatedly num #(rand-nth start-positions))]
    (map make-spline (map make-spline-vertices start-positions))))

(declare setup-pivot)
(defn make-main-splines [fields start-positions mulfn config]
  (let [splines (gen/make-field-splines fields start-positions mulfn config)]
    (swap! *state* assoc :splines splines)
    (setup-pivot)
    splines))

; Background vignette handling

(defonce vignette-texture (.load (THREE.TextureLoader.) "img/vignette.png"))

(defn- vignette-material [color]
  (THREE.MeshBasicMaterial. #js {:map vignette-texture
                                 :shading js/THREE.FlatShading
                                 :color (-> color tc/as-int32 :col)}))

(defn- setup-vignette [camera]
  (let [material (vignette-material tc/WHITE)
        geometry (THREE.PlaneGeometry. 60 30)
        plane (THREE.Mesh. geometry material)]
    (set! (.-x (.-position plane)) (.-x (.-position camera)))
    (set! (.-y (.-position plane)) (.-y (.-position camera)))
    (set! (.-z (.-position plane)) -40)
    (.add camera plane)
    (swap! *state* assoc :vignette plane)))

(defn- set-vignette-color [color]
  (let [new-material (vignette-material color)]
    (set! (.-material (:vignette @*state*)) new-material)))

; URL parsing

(defn split-nodes
  "Splits the nodes into background, external, and main nodes. Background nodes
  will get rendered as background, external ones will be rendered as thin lines
  wrapping around the main sculpture."
  [nodes]
  (let [render-context #(case (gtag/classify (first %))
                         :header :background
                         :external :external
                         :main)]
    (group-by render-context nodes)))

(defn process-response
  "Parses the given response, converts its DOM tree into a mesh, and adds the
  mesh to the current Three.js scene"
  [response]
  (let [{:keys [node-limit nodes-per-batch]} config
        body (:body response)
        seers (parse/map-occurences body (:all-seeing config))
        urls (set (parse/occurences body (:url-regex config)))
        all-nodes (parse/level-dom body)
        limited-nodes (take node-limit all-nodes)
        {:keys [background external main]} (split-nodes limited-nodes)
        part (fn [ns]
               (partition nodes-per-batch nodes-per-batch [] ns))
        fields (make-fields)
        start-positions (make-start-positions (count main))
        main-splines (make-main-splines fields start-positions mulfn config)
        ext-splines (make-external-splines start-positions (count external))
        bg-splines (make-background-splines start-positions (count background))
        main-palette (make-palette)
        ext-palette main-palette
        bg-palette (repeat tc/RED)
        set-lightness (fn [c l] (tc/hsla (tc/hue c) (tc/saturation c) l))]
    (println "URL: " (:url config))
    (println "Parsed nodes: " (count all-nodes))
    (println "URLs: " urls)
    (println "URL count: " (count urls))
    (println "Seers: " seers)
    (set-vignette-color (set-lightness (first main-palette) 0.25))
    (doseq [[nodes splines] (map vector (part background) (part bg-splines))]
      (async/put! (:seed-chan @*state*)
                  {:context :background
                   :params {:nodes nodes
                            :splines splines
                            :palette bg-palette}}))
    (doseq [[nodes splines] (map vector (part main) (part main-splines))]
      (async/put! (:seed-chan @*state*)
                  {:context :main
                   :params {:nodes nodes
                            :splines splines
                            :palette main-palette}}))
    (doseq [[nodes splines] (map vector (part external) (part ext-splines))]
      (async/put! (:seed-chan @*state*)
                  {:context :external
                   :params {:nodes nodes
                            :splines splines
                            :palette ext-palette}}))))

; Scene setup & loops

(defn seed-loop
  "Starts a go-loop which reads seeds from seed-chan, decides which processing
  function to use, and runs it with the parameters (:params) read from the seed.
  Reads from seed-chan are followed by a read from anim-chan, which must
  succeed before the rest of the logic runs. This way we're letting at least one
  animation frame to run before processing the next seed. The processing
  function puts its results into the mesh channel (accessible from *state*)."
  [seed-chan anim-chan]
  (go-loop
    []
    (let [seed (<! seed-chan)
          _ (<! anim-chan)]
      (spline-nodes->mesh (:params seed))
      (recur))))

(defn mesh-loop
  "Starts a go-loop which reads meshes from mesh-chan, converts them to Three.js
  meshes, and adds them to the scene."
  [mesh-chan]
  (go-loop
    []
    (let [mesh (<! mesh-chan)
          three-mesh (i/three-mesh mesh)]
      (.add (:scene @*state*) three-mesh)
      (recur))))

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
    (req/get-url (:url config) process-response)
    context))

(defn setup [initial-context]
  (let [seed-chan (async/chan 10)
        anim-chan (async/chan (async/dropping-buffer 1))
        mesh-chan (async/chan 10)
        camera (:camera initial-context)]
    (swap! *state* assoc :scene (:scene initial-context))
    (swap! *state* assoc :camera (:camera initial-context))
    (swap! *state* assoc :renderer (:renderer initial-context))
    (swap! *state* assoc :seed-chan seed-chan)
    (swap! *state* assoc :anim-chan anim-chan)
    (swap! *state* assoc :mesh-chan mesh-chan)
    (seed-loop seed-chan anim-chan)
    (mesh-loop mesh-chan)
    (setup-vignette camera)
    (on-reload initial-context)))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (.remove camera-pivot (:camera context))
    (on-reload context)))

(defn animate [context]
  (let [rot (.-x (.-rotation camera-pivot))]
    (set! (.-x (.-rotation camera-pivot)) (+ rot (:rotation-speed config)))
    (async/put! (:anim-chan @*state*) true)
    context))
