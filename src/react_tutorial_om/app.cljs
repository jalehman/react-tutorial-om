(ns react-tutorial-om.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan timeout]]
            [markdown.core :as md]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary]
            [cljs-http.client :as http]
            [react-tutorial-om.utils :refer [guid]])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util

(defn- with-id
  [m]
  (assoc m :id (guid)))

(defn- fetch-comments
  "The comments need to be a vector, not a list. Not sure why."
  [app opts]
  (go (let [{body :body} (<! (http/get (:url opts)))]
        (om/update! app #(assoc % :comments (vec (map with-id body)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Components

(def app-state
  (atom {:comments []}))

(defn comment-form []
  (om/component
   (dom/div {:className "commentForm"} "Hello, world! I am a commentForm.")))

(defn comment
  "Having some trouble getting the markdown to display properly. I've
   tried with:

    (dom/span {:dangerouslySetInnerHTML (js-obj {:__html raw-markup})})

   and other variants...but haven't gotten anywhere."

  [{:keys [author text] :as c} opts]
  (om/component
   (let [raw-markup (md/mdToHtml text)]
     (dom/div {:className "comment"}
              (dom/h2 {:className "commentAuthor"} author)
              raw-markup))))

(defn comment-list [app opts]
  (om/component
   (dom/div {:className "commentList"}
            (into-array
             (map #(om/build comment app
                             {:path [:comments %]
                              :key :id})
                  (range (count (:comments app))))))))

(defn comment-box [app opts]
  (reify
    om/IInitState
    (init-state [_ owner]
      (om/update! app #(assoc % :comments [])))
    om/IWillMount
    (will-mount [_ owner]
      (go (while true
            (fetch-comments app opts)
            (<! (timeout (:poll-interval opts))))))
    om/IRender
    (render [_ owner]
      (dom/div {:className "commentBox"}
               (dom/h1 nil "Comments")
               (om/build comment-list app)))))

(defn tutorial-app [app]
  (om/component
   (dom/div nil
            (om/build comment-box app
                      {:opts {:poll-interval 2000
                              :url "comments.json"}}))))

(om/root app-state tutorial-app (.getElementById js/document "content"))
