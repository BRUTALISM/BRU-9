(ns bru-9.scene
  (:require [bru-9.r01 :as r01]
            [bru-9.expanse :as expanse]))

;; Each sketch has a couple of hooks that tie into the main loop defined in this
;; namespace. When you want to switch drawing to a different sketch, just change
;; active-sketch-config to use a different key (and don't forget to add
;; sketch-specific hooks to the sketch-config map).
(def sketch-configs {:r01 {:setup-fn r01/setup
                           :animate-fn r01/animate}
                     :expanse {:setup-fn expanse/setup
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
    (set! (.-x (.-position camera)) 3)
    (set! (.-y (.-position camera)) 1)
    (set! (.-z (.-position camera)) 3)
    (.lookAt camera (THREE.Vector3. 0 0 0))
    (.updateProjectionMatrix camera)))

(defn on-resize []
  (let [renderer (:renderer @context)
        width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    (.setViewport renderer 0 0 width height)
    (set-camera-params)))

(defn on-click []
  )

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

    (.addEventListener js/window "resize" on-resize)
    (.addEventListener js/window "mousedown" on-click)

    ;; Run sketch-specific setup fn
    (reset! context ((:setup-fn active-sketch-config) @context))))

(defn run []
  (let [request-id (:request-id @context)]
    ;; Init
    (if (not @started) (do (start) (reset! started true)))

    ;; Kill the old animate function, if it exists
    (if request-id (.cancelAnimationFrame js/window request-id))

    ;; Register a new animate handler
    (.requestAnimationFrame js/window animate)))
