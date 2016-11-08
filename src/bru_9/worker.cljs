(ns bru-9.worker)

(defn process-msg [msg]
  (.postMessage js/self (str (.-data msg) " je morao biti ubiven")))

(set! (.-onmessage js/self) process-msg)