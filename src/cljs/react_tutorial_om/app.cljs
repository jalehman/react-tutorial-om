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
  (go (let [{{cs :comments} :body} (<! (http/get (:url opts)))]
        (om/update!
         app #(assoc % :comments (vec (map with-id cs)))))))

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

(def INITIAL [])

(def app-state
  (atom {:comments INITIAL}))

(defn comment
  [cursor owner opts]
  (.log js/console (str "comment cursor = " cursor))
  (om/component
    (let [text (:text cursor)
          author (:author cursor)
          raw-markup (md/mdToHtml (or text "blank comment!"))
          color "red"]
      (dom/div #js {:className "comment"}
               (dom/h2 #js {:className "commentAuthor"} author)
               (dom/span #js {:dangerouslySetInnerHTML #js {:__html raw-markup}} )))))

(defn comment-list
  [{:keys [comments] :as cursor} owner opts]
  ;; (.log js/console (str "comment-list cursor = " (into {} cursor)))
  ;; (.log js/console (str "owner = " (into {} owner)))
  (om/component
   (dom/div #js {:className "commentList"}
            (into-array
             (om/build-all comment comments
                                 {:key :id
                                  :fn (fn [c]
                                        ;; (.log js/console c) 
                                        c)
                                  :opts opts})))))

(defn save-comment!
  [comment cursor opts]
  (do (om/update! cursor 
                  (fn [comments] 
                    (conj comments (assoc comment :id (guid)))))
      (go (let [res (<! (http/post (:url opts) {:json-params comment}))]
            (prn (:message res))))))

(defn handle-submit
  [e cursor owner opts]
  (let [[author author-node] (value-from-node owner "author")
        [text text-node]     (value-from-node owner "text")]
    (.log js/console cursor)
    (when (and author text)
      (save-comment! {:author author :text text} cursor opts)
      (clear-nodes! author-node text-node))
    false))

(defn comment-form
  [app owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/form
       #js {:className "commentForm" :onSubmit #(handle-submit % app owner opts)}
       (dom/input #js {:type "text" :placeholder "Your Name" :ref "author"})
       (dom/input #js {:type "text" :placeholder "Say something..." :ref "text"})
       (dom/input #js {:type "submit" :value "Post"})))))
 
(defn comment-box 
  [cursor owner opts]
  (reify
    om/IInitState
    (init-state [this]
      (om/update! cursor #(assoc % :comments INITIAL)))
    om/IWillMount
    (will-mount [this]
      (go (while true
            (fetch-comments cursor opts)
            (<! (timeout (:poll-interval opts))))))
    om/IRender
    (render [this]
      (dom/div
       #js {:className "commentBox"}
       (dom/h1 nil "Comments")
       (om/build comment-list cursor)
       (om/build comment-form cursor {:opts opts})))))

(defn tutorial-app [cursor owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (om/build comment-box cursor
                         {:opts {:poll-interval 2000
                                 :url "/comments"}})))))

(om/root app-state tutorial-app (.getElementById js/document "content"))
