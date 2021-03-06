(ns bru-9.scenes.main
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [<! >!]]
            [bru-9.requests :as req]
            [bru-9.parse :as parse]
            [bru-9.geom.tag :as gtag]
            [bru-9.interop :as i]
            [bru-9.util :as u]
            [bru-9.url :as url]
            [bru-9.color.infinite :as ci]
            [bru-9.geom.generators :as gen]
            [bru-9.geom.bezier :as b]
            [bru-9.geom.vignette :as vig]
            [thi.ng.geom.webgl.glmesh :as glm]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]
            [thi.ng.color.core :as tc]
            [thi.ng.geom.core :as g]
            [bru-9.field.core :as f]
            [bru-9.color.core :as c]))

(def config {:url-regex "http(s)?://(\\w|-)+\\.((\\w|-)+\\.?)+"
             :url-options
             {:minimum-urls 5
              :filter-out
              ["facebook" "instagram" "wordpress" "twitter" "pinterest" "google"
               "apple" "github" "sourceforge" ".w3.org" ".wp.me" "gmpg.org"
               ".w.org" "snapchat" "vine.co" "ogp.me" "schema.org" "paypal"
               ".co.uk" "linkedin" "youtube"]}
             :default-urls ["http://slashdot.org" "http://reddit.com"]
             :node-limit 3000
             :nodes-per-batch 50
             :camera-distance 18
             :background-color 0x111111
             :start-positions-axis-following 1.6
             :start-positions-walk-multiplier 0.01
             :start-positions-count-mul 0.01
             :start-positions-random-offset 0.32
             :curve-tightness-min 0.04
             :curve-tightness-max 0.08
             :spline-hops-min 3
             :spline-hops-max 4
             :field-dimensions [10 5 5]
             :field-count 2
             :field-general-direction v/V3X
             :field-random-following 1.6
             :field-noise 1.4
             :mulfn-base 0.7
             :mulfn-jump-chance 0.08
             :mulfn-jump-intensity 0.8
             :wander-probability 0.24
             :default-spline-resolution 8
             :mesh-geometry-size 65535
             :palette-colors 2
             :base-brightnesses [1.0 0.25]
             :base-saturations [1.0 0.9]
             :hue-range 0.18
             :infinite-params {:hue 0.03
                               :saturation 0.1
                               :brightness 0.25}
             :rotation-speed 0.00015
             :vignette-inside-lightness 0.9
             :vignette-outside-lightness 0.7
             :vignette-saturation 1.0
             :background-points-per-spline 6
             :background-spline-tightness 0.05
             :background-spline-random-offset 0.14
             :content-spline-resolution 2
             :external-radius-min 1.0
             :external-radius-max 3.0
             :external-angle-min m/SIXTH_PI
             :external-angle-max m/HALF_PI
             :external-tightness-min 0.1
             :external-tightness-max 0.3
             :external-x-wobble 1.8
             :external-node-count 5
             :external-spline-resolution 16
             :url-spline-tightness 0.2
             :url-spline-hops 4
             :url-spline-multiplier 4.0
             :url-spline-resolution 14})

(defonce *state* (atom {}))

; Geometry generation

(defn- make-fields []
  (let [{:keys [field-count
                field-general-direction
                field-random-following
                field-dimensions
                field-noise]} config]
    (gen/make-fields field-count
                     field-dimensions
                     field-general-direction
                     field-random-following
                     field-noise)))

(defn make-start-positions [count]
  (let [{:keys [start-positions-axis-following
                start-positions-walk-multiplier
                start-positions-count-mul
                field-general-direction
                field-random-following
                field-dimensions]} config
        falloff (fn [x] (+ 0.6 (* 0.4 (u/tanh (- 2 (* x 0.1))))))
        mul (* start-positions-walk-multiplier
               (falloff (* start-positions-count-mul count)))
        mulfn (fn [_] mul)
        direction (m/* field-general-direction start-positions-axis-following)
        start-positions-field
        (gen/make-start-positions-field field-dimensions
                                        direction
                                        field-random-following)]
    (gen/make-start-positions start-positions-field count mulfn)))

(defn- mulfn [_]
  (let [{:keys [mulfn-base mulfn-jump-chance mulfn-jump-intensity]} config]
    (+ mulfn-base (if (< (rand) mulfn-jump-chance) mulfn-jump-intensity 0))))

(defn spline-resolution [tag]
  (case (gtag/classify (first tag))
    :content (:content-spline-resolution config)
    :outward (:url-spline-resolution config)
    :external (:external-spline-resolution config)
    (:default-spline-resolution config)))

(defn spline-nodes->mesh [{:keys [nodes splines palette]}]
  (let [gl-mesh (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        nodes-splines-colors (map vector nodes splines palette)
        tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline color (spline-resolution tag)))]
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

(defn make-url-splines [start-positions fields num]
  (let [tightness (:url-spline-tightness config)
        hops (:url-spline-hops config)
        multiplier (:url-spline-multiplier config)
        mulfn (fn [t] (* (+ 0.5 (* t 0.5)) multiplier))
        splinefn
        (fn [pos]
          (let [points (f/walk (rand-nth fields) pos hops mulfn)
                last-point (last points)
                second-last-point (nth points (- (count points) 2))
                last-vec (m/normalize (m/- last-point second-last-point))
                offset (m/normalize (v/vec3 0 (:y last-point) (:z last-point)))
                addition (m/* (m/+ last-vec offset) multiplier)
                added (m/+ last-point addition)
                all-points (conj points added)]
            (b/auto-spline3 all-points tightness)))]
    (take num (map splinefn (shuffle start-positions)))))

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

(defn- enqueue-batches [context nodes splines palette]
  (doseq [[ns ss] (map vector nodes splines)]
    (async/put! (:seed-chan @*state*)
                {:context context
                 :params {:nodes ns
                          :splines ss
                          :palette palette}})))

(defn- setup-vignette [palette]
  (let [vlin (:vignette-inside-lightness config)
        vlout (:vignette-outside-lightness config)
        vsat (:vignette-saturation config)]
    (vig/setup-vignette (:camera @*state*)
                        (vig/adjust-color (first palette) vsat vlin)
                        (vig/adjust-color (first palette) vsat vlout))))

(defn- create-splines [main external background urls]
  (let [fields (make-fields)
        start-positions (make-start-positions (count main))]
    [(make-main-splines fields start-positions mulfn config)
     (make-external-splines start-positions (count external))
     (make-background-splines start-positions (count background))
     (make-url-splines start-positions fields (count urls))]))

(defn- create-palettes []
  (let [base-palette (c/random-palette (:base-saturations config)
                                       (:base-brightnesses config)
                                       (:hue-range config))
        main-palette (ci/infinite-palette base-palette
                                          (:infinite-params config))
        ext-palette main-palette
        main-hue (tc/hue (first main-palette))
        bg-palette (ci/infinite-palette [(-> (tc/hsla main-hue 1.0 0.7)
                                             (tc/rotate-hue (* 0.66 m/PI)))]
                                        (:infinite-params config))
        url-palette main-palette]
    [base-palette main-palette ext-palette bg-palette url-palette]))

(defn- update-urls [new-urls]
  (let [current (:urls @*state*)
        updated-urls (url/append-urls current new-urls (:url-options config))]
    (swap! *state* assoc :urls updated-urls)))

(defn- generate-geometry [nodes urls]
  (let [{:keys [node-limit nodes-per-batch]} config
        part (fn [ns]
               (let [spl (split-at (/ (count ns) 2) ns)
                     rearr (interleave (reverse (first spl)) (second spl))]
                 (partition nodes-per-batch nodes-per-batch [] rearr)))
        limited-nodes (take node-limit nodes)
        {:keys [background external main]} (split-nodes limited-nodes)

        [main-splines ext-splines bg-splines url-splines]
        (create-splines main external background urls)

        [base-palette main-palette ext-palette bg-palette url-palette]
        (create-palettes)]
    (update-urls (map second urls))
    (setup-vignette base-palette)
    (doseq [[c ns ss p] [[:background (part background)(part bg-splines)
                          bg-palette]
                         [:main (part main) (part main-splines) main-palette]
                         [:external (part external) (part ext-splines)
                          ext-palette]
                         [:urls (part urls) (part url-splines) url-palette]]]
      (enqueue-batches c ns ss p))))

(declare process-response)
(defn- load-next-url []
  (let [url (url/current-url (:urls @*state*))]
    (println "Fetching" url)
    (req/get-url url process-response)))

(defn process-response [response]
  (if (= (:status response) 200)
    (let [body (:body response)
          nodes (parse/level-dom body)
          urls (map (fn [u] [:url u])
                    (set (parse/occurences body (:url-regex config))))]
      (if (> (count nodes) 0)
        (generate-geometry nodes urls)
        (load-next-url)))
    (load-next-url)))

; Scene setup & loops

(defn seed-loop
  "Starts a go-loop which reads seeds from seed-chan, decides which processing
  function to use, and runs it with the parameters (:params) read from the seed.
  Reads from seed-chan are followed by a read from anim-chan, which must
  succeed before the rest of the logic runs. This way we're letting at least one
  animation frame run before processing the next seed. The processing function
  puts its results onto the mesh channel (accessible from *state*)."
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
  (set! (.-children camera) #js [])
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
  (let []
    (load-next-url)
    context))

(defn setup [initial-context]
  (let [seed-chan (async/chan 10)
        anim-chan (async/chan (async/dropping-buffer 1))
        mesh-chan (async/chan 10)]
    (swap! *state* assoc :scene (:scene initial-context))
    (swap! *state* assoc :camera (:camera initial-context))
    (swap! *state* assoc :renderer (:renderer initial-context))
    (swap! *state* assoc :seed-chan seed-chan)
    (swap! *state* assoc :anim-chan anim-chan)
    (swap! *state* assoc :mesh-chan mesh-chan)
    (swap! *state* assoc :urls (url/init-urls (:default-urls config)))
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
