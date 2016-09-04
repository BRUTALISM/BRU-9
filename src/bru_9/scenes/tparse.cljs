(ns bru-9.scenes.tparse
  (:require [bru-9.requests :as req]
            [bru-9.parse :as parse]
            [bru-9.geom.tag :as gtag]
            [bru-9.interop :as i]
            [bru-9.util :as u]
            [bru-9.color.core :as c]
            [bru-9.color.infinite :as ci]
            [thi.ng.geom.webgl.glmesh :as glm]
            [clojure.zip :as zip]))

(def config {:urls ["http://cnn.com"]
             :url-regex "http(s)?://(\\w|-)+\\.((\\w|-)+\\.?)+"
             :all-seeing ["facebook" "google" "instagram" "twitter" "amazon"]})

(defonce *state* (atom {}))

(defn process-response
  "Parses the given response, converts its DOM tree into a mesh, and adds the
  mesh to the current Three.js scene"
  [response]
  (let [body (:body response)
        seers (parse/map-occurences body (:all-seeing config))
        urls (set (parse/occurences body (:url-regex config)))
        all-nodes (parse/level-dom body)
        limited-nodes (take 50 all-nodes)
        acc (glm/gl-mesh 65536 #{:col})
        palette (c/random-palette)
        infinite-palette (ci/infinite-palette palette {:hue 0.3
                                                       :saturation 0.2})
        nodes-colors (map vector limited-nodes infinite-palette)
        mesh (reduce #(gtag/tag->mesh %1 (first %2) (second %2))
                     acc nodes-colors)
        three-mesh (i/three-mesh mesh)
        scene (:scene @*state*)]
    (println "Parsed nodes: " (count all-nodes))
    (println "URLs: " urls)
    (println "URL count: " (count urls))
    (println "Seers: " seers)
    (.add scene three-mesh)))

(defn process-urls
  "Initiates an async load operation for all URLs in urls, and invokes
  process-response for each response received."
  [urls]
  (doseq [url urls] (req/get-url url process-response)))

;; Scene setup & main loop

(defn setup [initial-context]
  (let [scene (:scene initial-context)]
    (reset! *state* (assoc @*state* :scene scene))
    (process-urls (:urls config))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (process-urls (:urls config))))

(defn animate [context]
  (let []
    context))
