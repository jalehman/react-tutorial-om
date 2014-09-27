(defproject react-tutorial-om "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.reader "0.8.7"]
                 ;; CLJ
                 [ring/ring-core "1.2.2"]
                 [compojure "1.1.8"]
                 [cheshire "5.3.1"]
                 [clj-time "0.6.0"]
                 [ranking-algorithms "0.1.0-SNAPSHOT"]
                 [environ "1.0.0"]
                 [ring "1.2.2"]
                 [com.stuartsierra/component "0.2.2"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 ;; [secretary "0.7.1"]
                 [cljs-http "0.1.16"]
                 [om "0.7.1"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.0-SNAPSHOT"]
                 ]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-ring "0.8.7"]
            [lein-environ "1.0.0"]
            [lein-figwheel "0.1.4-SNAPSHOT"]]

  :ring {:handler react-tutorial-om.core/app
         :init    react-tutorial-om.core/init}

  :repl-options {:init-ns user}
  :main react-tutorial-om.core
  ;; :aot [react-tutorial-om.core]
  :source-paths ["src/clj" "src/cljs"]

  :profiles {:dev { :dependencies [[javax.servlet/servlet-api "2.5"]
                                   [reloaded.repl "0.1.0"]
                                   [org.clojure/tools.namespace "0.2.6"]]
                   :source-paths ["dev"]}

             :figwheel {:http-server-root "public" ;; resources/public
                        :port 3449 }}

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :externs ["om/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/app.js"
                                   :source-map "resources/public/js/app.js.map"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :output-wrapper false
                                   :preamble ["om/react.min.js"]
                                   :externs ["om/externs/react.js"]
                                   :closure-warnings
                                   {:non-standard-jsdoc :off}}}]})
