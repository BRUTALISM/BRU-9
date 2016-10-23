(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [bru-9.color.ptf :as cptf]
            [bru-9.util :as u]
            [bru-9.color.infinite :as ci]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.rect :as rect]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]))

(def config {:scaffolding-ratio 8.0})

; Classification and per-class configuration

(def class-configs
  {:header {:envelope-size 0.04}
   :external {:envelope-size 0.025}
   :scaffolding {:envelope-size 0.05}
   :content {:envelope-size 0.3}
   :outward {:envelope-size 0.028}
   :default {:envelope-size 0.02}})

(def classes
  {:header #{:html :head :meta :title :body}
   :external #{:script :link}
   :scaffolding #{:div :span :header :footer :noscript :style :nav :main :aside
                  :a :input :form :textarea}
   :content #{:h1 :h2 :h3 :h4 :h5 :h6 :p :b :code :pre :tt :ul :li
              :img :path :polygon :svg}
   ; Custom :url tag for URLs found w/ text search
   :outward #{:url}})

(defn classify
  "Returns the class (not related to CSS class) the given tag belongs to. See
  the classes map in this namespace for possible class values."
  [tag]
  (let [cs (reduce-kv #(if (%3 tag) (conj %1 %2) %1) #{} classes)]
    (if (empty? cs) :default (first cs))))

; Shape and envelope functions

(defmulti envelope
  "Returns the envelope function for the given tag."
  (fn [tag] (classify tag)))
(defmethod envelope :header [_]
  (u/saw 0.2 1.0))
(defmethod envelope :content [_]
  (u/saw 0.1 1.0))
(defmethod envelope :scaffolding [_]
  ;(let [; keep inner-power below 0.25
  ;      inner-power 0.18
  ;      ; exaggeration controls the difference between peaks and the valley of
  ;      ; the envelope – higher values make lines thinner in the middle
  ;      exaggeration 3.0]
  ;  (fn [t]
  ;    (u/pow (+ (u/sin (* m/PI (u/pow t inner-power)))
  ;              (u/sin (* m/PI (u/pow (- 1 t) inner-power))))
  ;           exaggeration)))

  ;(fn [t]
  ;  (if (< t 0.79)
  ;    (+ 0.2 (u/pow (* 1.12 t) 2.0))
  ;    (- 5.0 (* 5.0 t))))

  ;(fn [t] (+ 0.3 (* 0.7 (u/sin (* 3.5 (u/pow t 2.0))))))

  ;(fn [t] (+ 0.2 (u/pow (* 0.9 t) 2.0)))

  (fn [t] (+ 0.2 (* 0.8 t)))
  )
(defmethod envelope :outward [_]
  (fn [t] (- 1.0 t)))
(defmethod envelope :default [_]
  (u/saw 0.1 1.0))

(defmulti filter-spline
  "Filters the vertices of a spline according to tag class."
  (fn [tag] (classify tag)))
(defmethod filter-spline :content [_ vertices]
  (map #(u/nth01 vertices %) [0.0 0.1 0.4]))
(defmethod filter-spline :default [_ vertices]
  vertices)

(defn rotated-rect [{:keys [angle size ratio]}]
  (let [height size
        width (/ height ratio)
        zoff (v/vec3 0 0 (* (/ height 2.0) (u/sin angle)))
        [v0 v1 v2 v3] (map v/vec3 (g/vertices (rect/rect 0 0 width height)))]
    [(m/- v0 zoff)
     (m/- v1 zoff)
     (m/+ v2 zoff)
     (m/+ v3 zoff)]))

(defn triangle [{:keys [size]}]
  (let [h3 (* size 0.2886751347)
        a2 (* size 0.5)
        p0 (v/vec3 (- a2) (- h3) 0.0)
        p1 (v/vec3 a2 (- h3) 0.0)
        p2 (v/vec3 0.0 (* 2 h3) 0.0)]
    [p0 p1 p2]))

(defmulti ptf-frame
  "Returns the vertices of the PTF frame for the given tag."
  (fn [tag] (classify tag)))
(defmethod ptf-frame :header [_ params]
  (triangle params))
(defmethod ptf-frame :default [_ params]
  (rotated-rect params))

; Main PTF logic (per tag)

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a colored
  mesh, writing it into the given accumulator mesh."
  [acc tag spline color spline-resolution]
  (let [tag-key (first tag)
        tag-class (classify tag-key)
        class-config (get class-configs tag-class)
        {:keys [envelope-size]} class-config
        size (u/rand-magnitude envelope-size 0.5 0.0 10000000)
        max-angle m/HALF_PI
        points (filter-spline tag-key (g/vertices spline spline-resolution))
        idiv (-> points count dec double)
        envelope-fn (envelope tag-key)
        ratio (:scaffolding-ratio config)
        profilefn (fn [i]
                    (let [t (/ i idiv)
                          angle (* t max-angle (m/randnorm))
                          tsize (* size (envelope-fn t))]
                      (ptf-frame tag-key {:angle angle
                                          :size tsize
                                          :ratio ratio})))
        gradc (ci/next-color [color]
                             {:hue 0.02 :saturation 0.2 :brightness 0.2})
        colors (cptf/ptf-gradient-attribs color gradc 4 idiv)
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points
                    (map profilefn (range (count points)))
                    sweep-params)))
