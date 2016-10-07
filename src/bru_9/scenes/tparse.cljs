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
            [thi.ng.geom.circle :as cir]
            [thi.ng.geom.attribs :as attr]))

(def config {:url-regex "http(s)?://(\\w|-)+\\.((\\w|-)+\\.?)+"
             ;:url "http://brutalism.rs"
             ;:url "http://polumenta.zardina.org"
             ;:url "http://apple.com"
             :url "http://pitchfork.com"
             :all-seeing ["facebook" "google" "instagram" "twitter" "amazon"]
             :node-limit 5000
             :nodes-per-batch 100
             :camera-distance 14
             :background-color 0x111111
             :start-positions-axis-following 1.5
             :start-positions-walk-multiplier 0.015
             :start-positions-random-offset 0.5
             :external-radius-min 1.0
             :external-radius-max 2.5
             :external-angle-min m/QUARTER_PI
             :external-angle-max m/HALF_PI
             :external-tightness-min 0.1
             :external-tightness-max 1.0
             :external-x-wobble 0.5
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
             :mesh-geometry-size 131070
             :infinite-params {:hue 0.1
                               :saturation 0.2
                               :brightness -0.1}
             :rotation-speed 0.0                            ;0.0002
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
        infinite (ci/infinite-palette palette (:infinite-params config))]
    infinite))

(defn nodes->mesh [{:keys [nodes splines palette]} tagfn]
  (let [gl-mesh (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        nodes-splines-colors (map vector nodes splines palette)]
    (async/put! (:mesh-chan @*state*)
                (reduce tagfn gl-mesh nodes-splines-colors))))

; Background mesh generation

(defn background-nodes->mesh [{:keys [nodes splines palette]}]
  (let []
    ; TODO: Implement.
    nil))

; External mesh generation

(defn make-external-splines [start-positions num]
  (let [rmin (:external-radius-min config)
        rmax (:external-radius-max config)
        amin (:external-angle-min config)
        amax (:external-angle-max config)
        tmin (:external-tightness-min config)
        tmax (:external-tightness-max config)
        xw (:external-x-wobble config)
        angling
        (fn [a] (+ a (u/rand-range amin amax)))
        make-node
        (fn [p a]
          (let [r (u/rand-range rmin rmax)
                v (v/vec3 (u/rand-range (- xw) xw) 0 r)]
            (m/+ p (g/rotate-around-axis v v/V3X a))))
        make-spline-nodes
        (fn [start-pos]
          ; TODO: find out what's causing those huge spikes
          ; TODO: make 5 a param
          (map make-node (repeat 5 start-pos) (iterate angling 0)))
        make-spline
        (fn [nodes]
          (b/auto-spline3 nodes (u/rand-range tmin tmax)))
        start-positions (repeatedly num #(rand-nth start-positions))]
    (map make-spline (map make-spline-nodes start-positions))))

(defn external-nodes->mesh
  [params]
  ; TODO: 99% same as main-nodes->mesh, refactor (use different colors though)
  (let [tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline tc/YELLOW
                                (:spline-resolution config)))]
    (nodes->mesh params tagfn)))

; Main mesh generation

(declare setup-pivot)

(defn make-main-splines [fields start-positions mulfn config]
  (let [splines (gen/make-field-splines fields start-positions mulfn config)]
    (swap! *state* assoc :splines splines)
    (setup-pivot)
    splines))

(defn main-nodes->mesh [params]
  (let [tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline color
                                (:spline-resolution config)))]
    (nodes->mesh params tagfn)))

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
        palette (make-palette)]
    (println "Parsed nodes: " (count all-nodes))
    (println "URLs: " urls)
    (println "URL count: " (count urls))
    (println "Seers: " seers)
    (doseq [[nodes splines] (map vector (part main) (part main-splines))]
      (async/put! (:seed-chan @*state*)
                  {:context :main
                   :params {:nodes nodes
                            :splines splines
                            :palette palette}}))
    (doseq [[nodes splines] (map vector (part external) (part ext-splines))]
      (async/put! (:seed-chan @*state*)
                  {:context :external
                   :params {:nodes nodes
                            :splines splines
                            :palette palette}}))
    ; TODO: add seeds for background nodes
    ))

; Scene setup & loops

(defn seed-loop
  "Starts a go-loop which reads seeds from seed-chan, decides which processing
  function to use, and runs it with the parameters (:params) read from the seed.
  Reads from seed-chan are followed by a read from anim-chan, which must
  succeed before the rest of the logic runs. This way we're letting at least one
  animation frame to run before processing the next seed. The processing
  function puts its results into the mesh channel (accessible from *state*)."
  [seed-chan anim-chan]
  (let [proc-fns {:background background-nodes->mesh
                  :external external-nodes->mesh
                  :main main-nodes->mesh}]
    (go-loop
      []
      (let [{:keys [context params]} (<! seed-chan)
            _ (<! anim-chan)
            proc-fn (get proc-fns context)]
        (proc-fn params)
        (recur)))))

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
    (.setClearColor renderer background-color 1.0)
    (req/get-url (:url config) process-response)
    context))

(defn setup [initial-context]
  (let [seed-chan (async/chan 10)
        anim-chan (async/chan (async/dropping-buffer 1))
        mesh-chan (async/chan 10)]
    (swap! *state* assoc :scene (:scene initial-context))
    (swap! *state* assoc :camera (:camera initial-context))
    (swap! *state* assoc :seed-chan seed-chan)
    (swap! *state* assoc :anim-chan anim-chan)
    (swap! *state* assoc :mesh-chan mesh-chan)
    (seed-loop seed-chan anim-chan)
    (mesh-loop mesh-chan)
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
