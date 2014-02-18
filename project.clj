(defproject react-tutorial-om "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.8.2"]
                 ;; CLJ
                 [ring/ring-core "1.2.1"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]
                 [clj-time "0.6.0"]
                 [ranking-algorithms "0.1.0-SNAPSHOT"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 ;; [secretary "0.7.1"]
                 [cljs-http "0.1.7"]
                 [om "0.4.2"]]

  :plugins [[lein-cljsbuild "1.0.0"]
            [lein-ring "0.8.7"]]

  :ring {:handler react-tutorial-om.core/app
         :init    react-tutorial-om.core/init}

  :main react-tutorial-om.core
  ;; :aot [react-tutorial-om.core]
  :source-paths ["src/clj"]

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
