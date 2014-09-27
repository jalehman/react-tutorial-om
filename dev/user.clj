(ns user
  (:require [clojure.repl :refer :all]
            [reloaded.repl :refer [system init start stop go reset]]
            [react-tutorial-om.core :as core]
            [react-tutorial-om.system :as system]
            [cemerick.piggieback :as piggieback]
            [weasel.repl.websocket :as weasel]
            [clojure.tools.namespace.repl :refer (refresh)]))

(reloaded.repl/set-init! system/make-system)


(defn browser-repl []
  (piggieback/cljs-repl :repl-env (weasel/repl-env :ip "0.0.0.0" :port 9001)))
