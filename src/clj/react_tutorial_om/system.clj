(ns react-tutorial-om.system
  (:require [react-tutorial-om.core :as core]
            [com.stuartsierra.component :as component]))

(defn make-system []
  (component/system-map
   :webserver (core/new-webserver {:ring {:port 3000 :join? false}})))
