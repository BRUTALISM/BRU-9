(ns bru-9.requests
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn get-url [url callback]
  (go (let [req-chan (http/get "http://brutalism.rs")
            response (<! req-chan)]
        (callback response))))
