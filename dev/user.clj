(ns user
  (:require [clojure.java.io :as io]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [reloaded.repl :refer [system init start stop go reset]]
            [react-tutorial-om.core :as core]
            [react-tutorial-om.system :as system]
            [cemerick.piggieback :as piggieback]
            [weasel.repl.websocket :as weasel]))

(reloaded.repl/set-init! system/make-system)

(defn browser-repl []
  (piggieback/cljs-repl :repl-env (weasel/repl-env :ip "0.0.0.0" :port 9001)))
