(ns bru-9.url
  (:require [clojure.zip :as z]
            [cljs.core :as cljs]))

; URL handling and navigation logic

(defonce json-config (.require js/self "electron-json-config"))
(def default-urls #js ["http://creativeapplications.com"
                       "http://pitchfork.com"
                       "http://itsnicethat.com"
                       "http://nytimes.com"
                       "http://slashdot.org"])

; TODO: Remove visited URLs from the structure
; TODO: Forbid "deadend" URLs like facebook.com, instagram.com etc
; TODO: Don't load URLs which have less than X nodes

(defn init-urls
  "Returns a zipper containing the initial collection of URLs read from
  config.json. If the URLs are not present in config.json, they are written
  using the default-urls collection in this namespace."
  []
  (if-let [json-urls (.get json-config "urls")]
    (z/next (z/vector-zip [(shuffle (cljs/js->clj json-urls))]))
    (do
      ; First time loading of json config, write default URLs there
      (.set json-config "urls" default-urls)
      (z/next (z/vector-zip [(shuffle (cljs/js->clj default-urls))])))))

(defn current-url
  "Fetches the current URL pointed at by the url-zip zipper."
  [url-zip]
  (rand-nth (z/node url-zip)))

(defn- filter-urls [urls]
  ; TODO: Filter out subdomain-type URLs and leave only top-level ones.
  urls)

(defn append-urls
  "Appends urls to the url-zip zipper containing all the URLs so far. Returns
  the updated zipper."
  [url-zip urls]
  (let [filtered-urls (filter-urls urls)]
    (if (empty? filtered-urls)
      url-zip
      (-> url-zip
          (z/insert-right filtered-urls)
          z/right))))