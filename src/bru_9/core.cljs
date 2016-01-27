(ns bru-9.core
  (:require [bru-9.scene :as scene]))

(enable-console-print!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on your app
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (prn "JS reloaded (on-js-reload invoked)")
  (scene/run))

(scene/run)
