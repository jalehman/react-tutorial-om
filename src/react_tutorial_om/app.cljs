(ns react-tutorial-om.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType]))

(defn comment-box []
  (reify
    (om/IRender
      (render [_]
              (dom/div #js {:className "commentBox"})))))

(.log js/console "Hello, world!")
