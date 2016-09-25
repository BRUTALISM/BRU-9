(ns bru-9.scenes.core
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require [bru-9.scenes.throwups :as throwups]
            [bru-9.scenes.tthree :as tthree]
            [bru-9.scenes.tparse :as tparse]
            [bru-9.scenes.tfield :as tfield]
            [bru-9.scenes.tptf :as tptf]
            [bru-9.debug :as debug]
            [bru-9.interop :as interop]
            [cljs.core.async :as async :refer [<! >!]]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]))

(def config
  {:wasd-speed 0.1
   :mouse-sensitivity 0.4})

;; Each sketch has a couple of hooks that tie into the main loop defined in this
;; namespace. When you want to switch drawing to a different sketch, just change
;; active-sketch-config to use a different key (and don't forget to add
;; sketch-specific hooks to the sketch-config map).
(def sketch-configs {:three {:setup-fn tthree/setup
                             :animate-fn tthree/animate}
                     :parse {:setup-fn tparse/setup
                             :reload-fn tparse/reload
                             :animate-fn tparse/animate}
                     :field {:setup-fn tfield/setup
                             :reload-fn tfield/reload
                             :animate-fn tfield/animate}
                     :ptf {:setup-fn tptf/setup
                           :reload-fn tptf/reload
                           :animate-fn tptf/animate}
                     :throwups {:setup-fn throwups/setup
                                :reload-fn throwups/reload
                                :animate-fn throwups/animate}})
(def active-sketch-config (:parse sketch-configs))

(enable-console-print!)

(defonce context (atom {}))

(defn set-camera-params []
  (let [{:keys [camera canvas]} @context
        ratio (/ (.-clientWidth canvas) (.-clientHeight canvas))]
    (set! (.-fov camera) 40)
    (set! (.-aspect camera) ratio)
    (set! (.-near camera) 0.1)
    (set! (.-far camera) 1000)
    (.updateProjectionMatrix camera)))

(defn event-chan [elem event]
  (let [c (async/chan)]
    (.addEventListener elem event #(async/put! c %))
    c))

(defn resize-loop []
  (let [resize-chan (event-chan js/window "resize")
        {:keys [renderer canvas]} @context]
    (go
      (loop []
        (let [_ (<! resize-chan)
              pixel-ratio (or (.-devicePixelRatio js/window) 1.0)
              width (* (.-innerWidth js/window) pixel-ratio)
              height (* (.-innerHeight js/window) pixel-ratio)]
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

(declare reload)
(defn keyboard-loop []
  (let [keydown-chan (event-chan js/window "keydown")
        handlers {82 #(reload)}]
    (go
      (loop []
        (let [event (<! keydown-chan)
              code (.-keyCode event)]
          (if-let [handler (get handlers code)]
            (handler))
          (recur))))))

(defn wasd-loop []
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
  (let [canvas (:canvas @context)
        click-chan (event-chan canvas "mousedown")
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
  (let [canvas (.getElementById js/document "main_canvas")
        renderer (THREE.WebGLRenderer. #js {:canvas canvas
                                            :antialias true})
        scene (THREE.Scene.)
        camera (THREE.PerspectiveCamera. 0 0 0 0)
        light (THREE.PointLight. 0xffffff 1.5 20)
        pixel-ratio (or (.-devicePixelRatio js/window) 1.0)
        width (* (.-innerWidth js/window) pixel-ratio)
        height (* (.-innerHeight js/window) pixel-ratio)]

    (reset! context {:canvas canvas
                     :renderer renderer
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
    (wasd-loop)
    (mouse-loop)
    (keyboard-loop)

    ;; Run sketch-specific setup fn
    (reset! context ((:setup-fn active-sketch-config) @context))))

(defn reload []
  (let [canvas (.getElementById js/document "main_canvas")]
    (reset! context (assoc @context :canvas canvas))
    (if-let [reload-fn (:reload-fn active-sketch-config)]
      (reload-fn @context))))

(defn run []
  (let [request-id (:request-id @context)]
    ;; Init
    (if (not @started)
      (do (start) (reset! started true)))

    ;; Kill the old animate function, if it exists
    (if request-id (.cancelAnimationFrame js/window request-id))

    ;; Register a new animate handler
    (.requestAnimationFrame js/window animate)))
