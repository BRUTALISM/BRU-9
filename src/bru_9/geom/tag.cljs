(ns bru-9.geom.tag
  (:require [bru-9.geom.ptf :as ptf]
            [bru-9.color.ptf :as cptf]
            [bru-9.util :as u]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.rect :as rect]
            [thi.ng.color.core :as c]
            [thi.ng.math.core :as m]))

(def class-configs
  ; Potential params:
  ; * spline sampling resolution – smaller for short, large shapes
  ; * transport along subset of spline (for shorter shapes) – can also be
  ;   controlled via :spline-hops, but at this stage spline hops have already
  ;   been performed.
  ; * envelope shape – bulging, heavier shapes for short & important tag
  ;   classes, thin and pointy style (similar to current one) for others
  {:header {:envelope-size 0.2}
   :external {:envelope-size 0.3}
   :scaffolding {:envelope-size 0.1}
   :content {:envelope-size 0.5}
   :default {:envelope-size 0.1}})

(def classes
  {:header #{:html :head :meta :title :body}
   ; TODO: :external should be drawn onto separate fields (see Trello)
   :external #{:link :script}
   :scaffolding #{:div :span :header :footer :noscript :style :nav :main :aside}
   :content #{:h1 :h2 :h3 :h4 :h5 :h6 :p :a :b :code :pre :tt :input :ul :li
              :form :img :textarea}})

(defn all-classes [] (reduce #(into %1 %2) #{} (vals classes)))

(defn classify
  "Returns the set of classes (not related to CSS classes) the given tag belongs
  to. See the classes map in this namespace for possible class values."
  [tag]
  (let [cs (reduce-kv #(if (%3 tag) (conj %1 %2) %1) #{} classes)]
    (if (empty? cs)
      (do
        (println "Tag not classified: " tag)
        #{:default})
      cs)))

(defn envelope [t]
  ; TODO: Implement a smarter algo. Different one for each tag class.
  (+ 0.2 (* t 0.8)))

(defn tag->mesh
  "Converts a given Hiccup node representing one DOM element into a colored
  mesh, writing it into the given accumulator mesh."
  [acc tag spline color spline-resolution]
  (let [points (g/vertices spline spline-resolution)
        tag-class (first (classify (first tag)))
        class-config (get class-configs tag-class)
        size (u/rand-magnitude (:envelope-size class-config) 0.1 0.0 10000000)
        max-angle m/HALF_PI
        steps (dec (count points))
        rotated-rect
        (fn [angle size]
          (let [h size
                w (/ h 4)
                zoff (v/vec3 0 0 (* (/ h 2) (u/sin angle)))
                [v0 v1 v2 v3] (map v/vec3 (g/vertices (rect/rect 0 0 w h)))]
            [(m/- v0 zoff)
             (m/- v1 zoff)
             (m/+ v2 zoff)
             (m/+ v3 zoff)]))
        profilefn (fn [i]
                    (let [t (/ i steps)
                          angle (+ (- max-angle) (* 2 t max-angle))
                          tsize (* size (envelope t))]
                      (rotated-rect angle tsize)))
        gradc (c/random-analog color 0.3)
        colors (cptf/ptf-gradient-attribs color gradc 4 steps)
        sweep-params {:mesh acc
                      :attribs {:col colors}}]
    (ptf/sweep-mesh points
                    (map profilefn (range steps))
                    sweep-params)))
