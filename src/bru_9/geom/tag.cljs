(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [bru-9.color.ptf :as cptf]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.rect :as rect]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]))

; Classification and per-class configuration

(def class-configs
  ; Potential params:
  ; * spline sampling resolution – smaller for short, large shapes
  ; * transport along subset of spline (for shorter shapes) – can also be
  ;   controlled via :spline-hops, but at this stage spline hops have already
  ;   been performed.
  ; * envelope shape – bulging, heavier shapes for short & important tag
  ;   classes, thin and pointy style (similar to current one) for others
  {:header {:envelope-size 0.2}
   :external {:envelope-size 0.12}
   :scaffolding {:envelope-size 0.05}
   :content {:envelope-size 0.4}
   :default {:envelope-size 0.2}})

(def classes
  {:header #{:html :head :meta :title :body}
   ; TODO: :external should be drawn onto separate fields (see Trello)
   :external #{:link :script}
   :scaffolding #{:div :span :header :footer :noscript :style :nav :main :aside
                  :a :input :form :textarea}
   :content #{:h1 :h2 :h3 :h4 :h5 :h6 :p :b :code :pre :tt :ul :li
              :img}})

(defn all-classes [] (reduce #(into %1 %2) #{} (vals classes)))

(defn classify
  "Returns the class (not related to CSS class) the given tag belongs to. See
  the classes map in this namespace for possible class values."
  [tag]
  (let [cs (reduce-kv #(if (%3 tag) (conj %1 %2) %1) #{} classes)]
    (if (empty? cs)
      (do
        (println "Tag not classified: " tag)
        :default)
      (first cs))))

; Shape and envelope functions

; Returns the envelope function for the given tag
(defmulti envelope (fn [tag] (classify tag)))
(defmethod envelope :content [_]
  (u/saw 0.1 1.0))
(defmethod envelope :default [_]
  (fn [t] (+ 0.2 (* t 0.8))))

; Filters the vertices of a spline according to tag class
(defmulti filter-spline (fn [tag] (classify tag)))
(defmethod filter-spline :content [_ vertices]
  (map #(u/nth01 vertices %) [0.0 0.2 1.0]))
(defmethod filter-spline :default [_ vertices]
  vertices)

(defn rotated-rect [angle size]
  (let [height size
        width (/ height 4)
        zoff (v/vec3 0 0 (* (/ height 2) (u/sin angle)))
        [v0 v1 v2 v3] (map v/vec3 (g/vertices (rect/rect 0 0 width height)))]
    [(m/- v0 zoff)
     (m/- v1 zoff)
     (m/+ v2 zoff)
     (m/+ v3 zoff)]))

; Main PTF logic (per tag)

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a colored
  mesh, writing it into the given accumulator mesh."
  [acc tag spline color spline-resolution]
  (let [tag-key (first tag)
        tag-class (classify tag-key)
        class-config (get class-configs tag-class)
        {:keys [envelope-size]} class-config
        size (u/rand-magnitude envelope-size 0.1 0.0 10000000)
        max-angle m/HALF_PI
        points (filter-spline tag-key (g/vertices spline spline-resolution))
        idiv (-> points count dec double)
        envelope-fn (envelope tag-key)
        profilefn (fn [i]
                    (let [t (/ i idiv)
                          angle (+ (- max-angle) (* 2 t max-angle))
                          tsize (* size (envelope-fn t))]
                      (rotated-rect angle tsize)))
        gradc (c/random-analog color 0.2)
        colors (cptf/ptf-gradient-attribs color gradc 4 idiv)
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points
                    (map profilefn (range (count points)))
                    sweep-params)))
