(ns bru-9.core
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require [bru-9.scenes.core :as scene]
            [bru-9.werk :as w]
            [cljs.core.async :as async :refer [<! >!]]
            [reagent.core :as reagent]))

(enable-console-print!)

(defn main-page []
  [:p ""])

(defn mount-root []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on your app
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (prn "JS reloaded (on-js-reload invoked)")
  (mount-root)
  (scene/reload))

(defn init! []
  (let [[to-chan from-chan] (w/worker "js/worker/worker.js")]
    (go (>! to-chan "Pirke"))
    (go (println (<! from-chan)))
    (mount-root)
    (scene/run)))
