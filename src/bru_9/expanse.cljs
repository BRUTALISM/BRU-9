(ns bru-9.expanse
  (:require [bru-9.requests :as req]
            [bru-9.parsing :as parse]
            [bru-9.geom.tags :as gtag]
            [thi.ng.geom.webgl.glmesh :as glm]
            [bru-9.interop :as i]
            [clojure.zip :as zip]))

(def config {:urls ["http://google.com"]})

(defonce *state* (atom {}))

(defn process-response
  "Parses the given response, converts its DOM tree into a mesh, and adds the
  mesh to the current Three.js scene"
  [response]
  (let [zipped (parse/zip-html (:body response))
        limited-nodes (take 20 (parse/depth-seq zipped))
        acc (glm/gl-mesh 65536 #{:col})
        mesh (reduce gtag/tag->mesh acc limited-nodes)
        three-mesh (i/three-mesh mesh)
        scene (:scene @*state*)]
    (.add scene three-mesh)))

(defn process-urls
  "Initiates an async load operation for all URLs in urls, and invokes
  process-response for each response received."
  [urls]
  (doseq [url urls] (req/get-url url process-response)))

(defn clear-scene [scene]
  (set! (.-children scene) #js []))

;; Scene setup & main loop

(defn setup [initial-context]
  (let [scene (:scene initial-context)]
    (reset! *state* (assoc @*state* :scene scene))
    (process-urls (:urls config))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (clear-scene scene)
    (process-urls (:urls config))))

(defn animate [context]
  (let []
    context))
