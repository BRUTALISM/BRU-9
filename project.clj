(defproject bru-9 "1.0"
  :description "An alternative renderer for the web."
  :url "http://brutalism.rs"
  :license {:name "Attribution-NonCommercial-ShareAlike 4.0 International"
            :url "https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-http "0.1.39"]
                 [hickory "0.6.0"]
                 [thi.ng/geom "0.0.1046"]
                 [thi.ng/color "1.1.1"]
                 [thi.ng/typedarrays "0.1.3"]
                 [thi.ng/math "0.2.1"]
                 [figwheel-sidecar "0.5.4-6"]
                 [cljsjs/react "15.3.1-0"]
                 [cljsjs/nodejs-externs "1.0.4-1"]
                 [reagent "0.6.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.6"]]
  :cljsbuild
  {:builds
   {:app {:source-paths ["src"]
          :compiler {:output-to "app/js/p/app.js"
                     :output-dir "app/js/p/out"
                     :asset-path "js/p/out"
                     :optimizations :none
                     :pretty-print true
                     :cache-analysis true}}}}

  :hooks [leiningen.cljsbuild]
  :source-paths ["src" "script"]
  :clean-targets [:target-path "out" "app/js/p"]
  :figwheel {:css-dirs ["app/css"]
             :nrepl-port 7888}

  :profiles
  {:dev
   {:dependencies [[com.cemerick/piggieback "0.2.1"]
                   [org.clojure/tools.nrepl "0.2.11"]
                   [figwheel-sidecar "0.5.4-6"]]
    :plugins [[lein-ancient "0.6.8"]
              [lein-kibit "0.1.2"]
              [lein-cljfmt "0.4.1"]
              [lein-figwheel "0.5.6"]]
    :cljsbuild
    {:builds
     {:app {:source-paths ["env/dev/cljs"]
            :compiler {:source-map true
                       :main "bru-9.dev"
                       :verbose true}
            :figwheel {:on-jsload "bru-9.core/on-js-reload"}}}}
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

   :production
   {:cljsbuild
    {:builds
     {:app {:source-paths ["env/prod/cljs"]
            :compiler {:optimizations :simple
                       :main "bru-9.prod"
                       :parallel-build true
                       :cache-analysis false
                       :closure-defines {"goog.DEBUG" false}
                       :externs ["externs/misc.js"]
                       :pretty-print false}}}}}})
