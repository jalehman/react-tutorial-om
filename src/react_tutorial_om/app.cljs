(ns react-tutorial-om.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! chan]]
            [markdown.core :as md]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary]
            [react-tutorial-om.utils :refer [guid]])
  (:import [goog History]
           [goog.history EventType]))

;; Because swannodette did...
(enable-console-print!)

(def app-state
  (atom {:comments
         [{:author "Pete Hunt" :text "This is one comment." :id (guid)}
          {:author "Jordan Walke" :text "This is *another* comment." :id (guid)}]}))

(defn comment-form []
  (om/component
   (dom/div {:className "commentForm"} "Hello, world! I am a commentForm.")))

;; Is this the right way to do things? In the tutorial they use
;; {this.props.children} instead of passing the comment as an
;; argument. How would I go about that?

;; Can't seem to get this to work. Moving on.
;; (dom/span {:dangerouslySetInnerHTML (js-obj {:__html raw-markup})})
(defn comment
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
  (om/component
   (dom/div {:className "commentBox"}
            (dom/h1 nil "Comments")
            (om/build comment-list app))))

(om/root app-state comment-box (.getElementById js/document "content"))
