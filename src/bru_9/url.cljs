(ns bru-9.url
  (:require [clojure.zip :as z]
            [cljs.core :as cljs]
            [clojure.string :as str]))

; URL handling and navigation logic

(defonce json-config (.require js/self "electron-json-config"))

; TODO: Remove visited URLs from the structure
; TODO: Forbid "deadend" URLs like facebook.com, instagram.com etc
; TODO: Don't load URLs which have less than X nodes

(defn init-urls
  "Returns a zipper containing the initial collection of URLs read from
  config.json. If the URLs are not present in config.json, they are written
  using the supplied urls collection."
  [urls]
  (if-let [json-urls (.get json-config "urls")]
    (z/next (z/vector-zip [(shuffle (cljs/js->clj json-urls))]))
    (do
      ; First time loading of json config, write default URLs there
      (.set json-config "urls" (cljs/clj->js urls))
      (z/next (z/vector-zip [(shuffle urls)])))))

(defn current-url
  "Fetches the current URL pointed at by the url-zip zipper."
  [url-zip]
  (rand-nth (z/node url-zip)))

(defn- prepare-urls [urls filter-keywords]
  (let [forbidden
        (fn [url]
          (not (reduce #(or %1 (str/includes? url %2)) false filter-keywords)))
        top-domain
        (fn [url]
          (let [protocol-sep "://"
                addr-sep "."
                [protocol addr] (str/split url protocol-sep)
                addr-parts (str/split addr addr-sep)
                top-addr (nthrest addr-parts (- (count addr-parts) 2))
                www (into ["www"] top-addr)]
            (str/join protocol-sep [protocol (str/join addr-sep www)])))
        to-http
        (fn [url]
          (let [protocol-sep "://"
                [_ addr] (str/split url protocol-sep)]
            (str/join protocol-sep ["http" addr])))
        prepare (comp (filter forbidden)
                      (map top-domain)
                      (map to-http))]
    (vec (into #{} prepare urls))))

(defn append-urls
  "Appends urls to the url-zip zipper containing all the URLs so far. Returns
  the updated zipper."
  [url-zip urls {:keys [minimum-urls filter-out]}]
  (let [prepared-urls (prepare-urls urls filter-out)]
    (if (< (count prepared-urls) minimum-urls)
      url-zip
      (-> url-zip
          (z/insert-right prepared-urls)
          z/right))))