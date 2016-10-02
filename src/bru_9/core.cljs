(ns bru-9.core
  (:require [bru-9.scenes.core :as scene]
            [reagent.core :as reagent]))

(enable-console-print!)

(defn main-page []
  [:span.title "PROTOTYPE"])

(defn mount-root []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on your app
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (prn "JS reloaded (on-js-reload invoked)")
  (mount-root)
  (scene/reload))

(defn init! []
  (mount-root)
  (scene/run))
