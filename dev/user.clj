(ns user
  (:require [clojure.repl :refer :all]
            [reloaded.repl :refer [system init start stop go reset]]
            [react-tutorial-om.core :as core]
            [clojure.tools.namespace.repl :refer (refresh)]))

(reloaded.repl/set-init! core/make-system)
