(ns bru-9.scenes.core
  (:require-macros
   [cljs.core.async.macros :refer [go alt!]])
  (:require [bru-9.scenes.tthree :as tthree]
            [bru-9.scenes.tparse :as tparse]
            [bru-9.scenes.tfield :as tfield]
            [bru-9.scenes.tptf :as tptf]
            [bru-9.debug :as debug]
            [bru-9.interop :as interop]
            [cljs.core.async :as async :refer [<! >!]]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]))

(def config
  {:wasd-speed 0.5
   :mouse-sensitivity 0.4})

;; Each sketch has a couple of hooks that tie into the main loop defined in this
;; namespace. When you want to switch drawing to a different sketch, just change
;; active-sketch-config to use a different key (and don't forget to add
;; sketch-specific hooks to the sketch-config map).
(def sketch-configs {:tthree {:setup-fn tthree/setup
                              :animate-fn tthree/animate}
                     :tparse {:setup-fn tparse/setup
                              :reload-fn tparse/reload
                              :animate-fn tparse/animate}
                     :tfield {:setup-fn tfield/setup
                              :reload-fn tfield/reload
                              :animate-fn tfield/animate}
                     :tptf {:setup-fn tptf/setup
                            :reload-fn tptf/reload
                            :animate-fn tptf/animate}})
(def active-sketch-config (:tptf sketch-configs))

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
  (let [keydown-chan (event-chan js/window "keydown")
        keyup-chan (event-chan js/window "keyup")
        dirmap {87 (v/vec3 0 0 -1)
                83 (v/vec3 0 0 1)
                65 (v/vec3 -1 0 0)
                68 (v/vec3 1 0 0)}]
    (go
     (loop [directions #{}]
       (let [[event ch] (async/alts! [keydown-chan keyup-chan])
             code (.-keyCode event)
             camera (:camera @context)
             new-directions (if-let [dir (get dirmap code)]
                              (if (= ch keydown-chan)
                                (conj directions dir)
                                (disj directions dir))
                              directions)
             camera-movement (m/normalize
                              (reduce m/+ (v/vec3 0) new-directions))]
         (reset! context (assoc @context :camera-movement camera-movement))
         (recur new-directions))))))

(defn mouse-loop []
  (let [click-chan (event-chan canvas "mousedown")
        move-chan (event-chan canvas "mousemove")
        end-chan (async/merge [(event-chan canvas "mouseout")
                               (event-chan canvas "mouseleave")
                               (event-chan canvas "mouseup")])]
    (go
     (loop []
       (let [[e1 ch] (async/alts! [click-chan move-chan end-chan])]
         (if (= ch click-chan)
           (loop [lastX (.-clientX e1)
                  lastY (.-clientY e1)]
             (alt!
              move-chan
              ([e2] (let [sens (:mouse-sensitivity config)
                          x (.-clientX e2)
                          y (.-clientY e2)
                          xrot (* sens (- lastX x))
                          yrot (* sens (- lastY y))]
                      (reset! context
                              (assoc @context :camera-rotation [xrot yrot]))
                      (recur x y)))
              end-chan true)))
         (recur))))))

(defn update-camera []
  (let [direction (:camera-movement @context)]))

(defn animate []
  (let [request-id (.requestAnimationFrame js/window animate)
        camera (:camera @context)
        movement (:camera-movement @context (v/vec3 0))
        rotation (:camera-rotation @context [0 0])]
    (reset! context (conj @context {:request-id request-id}))

    ;; Rotate the camera
    (interop/rotate-camera camera rotation)
    (reset! context (assoc @context :camera-rotation [0 0]))

    ;; Move the camera
    (interop/move-camera camera (m/* movement (:wasd-speed config)))

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
        pixel-ratio (or (.-devicePixelRatio js/window) 1.0)
        width (* (.-innerWidth js/window) pixel-ratio)
        height (* (.-innerHeight js/window) pixel-ratio)]

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
