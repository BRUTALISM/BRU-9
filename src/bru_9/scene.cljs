(ns bru-9.scene
  (:require-macros
   [cljs.core.async.macros :refer [go alt!]])
  (:require [bru-9.r01 :as r01]
            [bru-9.expanse :as expanse]
            [bru-9.debug :as debug]
            [bru-9.interop :as interop]
            [cljs.core.async :as async :refer [<! >!]]
            [thi.ng.geom.vector :as v]))

;; Each sketch has a couple of hooks that tie into the main loop defined in this
;; namespace. When you want to switch drawing to a different sketch, just change
;; active-sketch-config to use a different key (and don't forget to add
;; sketch-specific hooks to the sketch-config map).
(def sketch-configs {:r01 {:setup-fn r01/setup
                           :animate-fn r01/animate}
                     :expanse {:setup-fn expanse/setup
                               :reload-fn expanse/reload
                               :animate-fn expanse/animate}})
(def active-sketch-config (:expanse sketch-configs))

(enable-console-print!)

;; The vertical extent of the canvas in world space (this is basically defining
;; your visible portion of the scene in vertical units - horizontal extents
;; depend on window's aspect ratio).
(def ymax 2)

(def canvas (.getElementById js/document "main_canvas"))

(defonce context (atom {}))

(defn set-camera-params []
  (let [camera (:camera @context)
        ratio (/ (.-clientWidth canvas) (.-clientHeight canvas))
        xmax (* ymax ratio)
        xmax2 (/ xmax 2)
        ymax2 (/ ymax 2)]
    (set! (.-fov camera) 40)
    (set! (.-aspect camera) ratio)
    (set! (.-near camera) 0.1)
    (set! (.-far camera) 1000)
    (set! (.-x (.-position camera)) 30)
    (set! (.-y (.-position camera)) 10)
    (set! (.-z (.-position camera)) 30)
    (.lookAt camera (THREE.Vector3. 0 0 0))
    (.updateProjectionMatrix camera)))

(defn event-chan [elem event]
  (let [c (async/chan)]
    (.addEventListener elem event #(async/put! c %))
    c))

(defn resize-loop []
  (let [resize-chan (event-chan js/window "resize")
        renderer (:renderer @context)]
    (go
     (loop []
       (let [_ (<! resize-chan)
             width (.-innerWidth js/window)
             height (.-innerHeight js/window)]
         (set! (.-width canvas) width)
         (set! (.-height canvas) height)
         (.setViewport renderer 0 0 width height)
         (set-camera-params)
         (recur))))))

(defn debug-loop []
  (go
   (loop []
     (.add (:scene @context) (interop/debug->mesh (<! debug/channel)))
     (recur))))

(defn keyboard-loop []
  (let [key-chan (event-chan js/window "keypress")]
    (go
     (loop []
       (let [event (<! key-chan)
             code (.-keyCode event)
             camera (:camera @context)]
         (cond
          (= code 119) (interop/move-camera camera (v/vec3 0 0 1))
          (= code 115) (interop/move-camera camera (v/vec3 0 0 -1))
          (= code 97) (interop/move-camera camera (v/vec3 -1 0 0))
          (= code 100) (interop/move-camera camera (v/vec3 1 0 0)))
         (recur))))))

(defn mouse-loop []
  (let [click-chan (event-chan canvas "mousedown")
        move-chan (event-chan canvas "mousemove")
        end-chan (async/merge [(event-chan canvas "mouseout")
                               (event-chan canvas "mouseleave")
                               (event-chan canvas "mouseup")])
        random-line #(debug/line (thi.ng.geom.vector/randvec3 (+ 2 (rand 10)))
                                 (thi.ng.geom.vector/randvec3 (+ 2 (rand 10)))
                                 thi.ng.color.core/RED)]
    (go
     (loop []
       (let [[_ ch] (async/alts! [click-chan move-chan end-chan])]
         (if (= ch click-chan)
           (loop []
             (alt!
              move-chan ([_] (do (random-line) (recur)))
              end-chan true)))
         (recur))))))

(defn animate []
  (let [request-id (.requestAnimationFrame js/window animate)]
    (reset! context (conj @context {:request-id request-id}))

    ;; Call sketch-specific animate fn
    (reset! context ((:animate-fn active-sketch-config) @context))

    ;; Render everything
    (.render (:renderer @context) (:scene @context) (:camera @context))))

;; Figwheel live reloading hack - the start fn should be invoked only once
(defonce started (atom false))

(defn start []
  (let [renderer (THREE.WebGLRenderer. #js {:canvas canvas})
        scene (THREE.Scene.)
        camera (THREE.PerspectiveCamera. 0 0 0 0)
        light (THREE.PointLight. 0xffffff 1.5 20)

        width (.-innerWidth js/window)
        height (.-innerHeight js/window)]

    (reset! context {:renderer renderer
                     :scene scene
                     :camera camera})

    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    (.setViewport renderer 0 0 width height)

    (set-camera-params)

    (.set (.-position light) 1 1 1)
    (.add scene light)

    (resize-loop)
    (debug-loop)
    (keyboard-loop)
    (mouse-loop)

    ;; Run sketch-specific setup fn
    (reset! context ((:setup-fn active-sketch-config) @context))))

(defn reload []
  (if-let [reload-fn (:reload-fn active-sketch-config)]
    (reload-fn @context)))

(defn run []
  (let [request-id (:request-id @context)]
    ;; Init
    (if (not @started)
      (do (start) (reset! started true)))

    ;; Kill the old animate function, if it exists
    (if request-id (.cancelAnimationFrame js/window request-id))

    ;; Register a new animate handler
    (.requestAnimationFrame js/window animate)))
