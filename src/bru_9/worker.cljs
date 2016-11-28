(ns bru-9.worker
  (:require [bru-9.geom.tag :as gtag]
            [thi.ng.geom.webgl.glmesh :as glm]))

(defn spline-resolution [tag config]
  (case (gtag/classify (first tag))
    :content (:content-spline-resolution config)
    :outward (:url-spline-resolution config)
    :external (:external-spline-resolution config)
    (:default-spline-resolution config)))

(defn spline-nodes->mesh [{:keys [nodes splines palette config]}]
  (let [gl-mesh (glm/gl-mesh (:mesh-geometry-size config) #{:col})
        nodes-splines-colors (map vector nodes splines palette)
        tagfn (fn [acc [tag spline color]]
                (gtag/tag->mesh acc tag spline color
                                (spline-resolution tag config)))]
    (reduce tagfn gl-mesh nodes-splines-colors)))

(defn process-msg [msg]
  (.log js/console (str "Received " (.-data msg)))
  (.postMessage js/self (clj->js (spline-nodes->mesh (js->clj (.-data msg))))))

(.log js/console "Starting webworker.")
(set! (.-onmessage js/self) process-msg)