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

(defn- value-from-node
  [owner field]
  (let [n (om/get-node owner field)
        v (-> n .-value clojure.string/trim)]
    (when-not (empty? v)
      [v n])))

(defn- clear-nodes!
  [& nodes]
  (doall (map #(set! (.-value %) "") nodes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Components

(def app-state
  (atom {:comments []}))

(defn comment
  "Having some trouble getting the markdown to display properly. I've
   tried with:

    (dom/span {:dangerouslySetInnerHTML (js-obj {:__html raw-markup})})

   and other variants...but haven't gotten anywhere."

  [{:keys [author text] :as c} opts]
  (om/component
   (let [raw-markup (md/mdToHtml text)]
     (dom/div #js {:className "comment"}
              (dom/h2 #js {:className "commentAuthor"} author)
              raw-markup))))

(defn comment-list [app opts]
  (om/component
   (dom/div #js {:className "commentList"}
            (into-array
             (map #(om/build comment app
                             {:path [:comments %]
                              :key :id})
                  (range (count (:comments app))))))))

(defn save-comment!
  [comment app opts]
  (do (om/update! app [:comments]
                  (fn [comments] (conj comments (assoc comment :id (guid)))))
      (go (let [res (<! (http/post (:url opts)
                                   {:body comment}))]
            (prn res)))))

(defn handle-submit
  [e app owner opts]
  (let [[author author-node] (value-from-node owner "author")
        [text text-node]     (value-from-node owner "text")]
    (when (and author text)
      (save-comment! {:author author :text text} app opts)
      (clear-nodes! author-node text-node))
    false))

(defn comment-form
  [app opts]
  (reify
    om/IRender
    (render [_ owner]
      (dom/form
       #js {:className "commentForm" :onSubmit #(handle-submit % app owner opts)}
       (dom/input #js {:type "text" :placeholder "Your Name" :ref "author"})
       (dom/input #js {:type "text" :placeholder "Say something..." :ref "text"})
       (dom/input #js {:type "submit" :value "Post"})))))

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
      (dom/div
       #js {:className "commentBox"}
       (dom/h1 nil "Comments")
       (om/build comment-list app)
       (om/build comment-form app {:opts opts})))))

(defn tutorial-app [app]
  (reify
    ;; om/IWillMount
    ;; (will-mount [_ owner]
    ;;   (let [events (chan)]
    ;;     (om/set-state! owner [:events] events)
    ;;     (go (while true
    ;;           (handle-event app (<! events))))))
    om/IRender
    (render [_ owner]
      (dom/div nil
               (om/build comment-box app
                         {:opts {:poll-interval 2000
                                 :url "comments.json"}})))))

(om/root app-state tutorial-app (.getElementById js/document "content"))
