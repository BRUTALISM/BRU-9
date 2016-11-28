(ns bru-9.werk
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! >!]]))

(defn worker
  "Spawns a new webworker from the given file path. Returns a [to-chan
  from-chan] pair of channels, which are used to communicate with the worker."
  [path]
  (let [worker (js/Worker. path)
        to-chan (async/chan 1)
        from-chan (async/chan 1)
        on-msg (fn [msg] (go (>! from-chan (js->clj (.-data msg)))))]
    (set! (.-onmessage worker) on-msg)
    (go
      (loop []
        (let [msg (<! to-chan)]
          (println "Posting message:" msg)
          (.postMessage worker (clj->js msg))
          (recur))))
    [to-chan from-chan]))
