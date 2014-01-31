(ns react-tutorial-om.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   ;; #_[secretary.macros :refer [defroute]]
                   [react-tutorial-om.utils :refer [logm make-table-cols]]
                   )
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan timeout]]
            [markdown.core :as md]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            #_[secrtary.core :as secretary]
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
        (when cs
          (om/transact!
           app #(assoc % :comments (vec (map with-id cs))))))))

(defn- fetch-rankings
  "The comments need to be a vector, not a list. Not sure why."
  [app opts]
  (go (let [{{ranks :rankings} :body status :status} (<! (http/get (:url opts)))]
        (when ranks
          (om/transact!
           app #(assoc % :rankings (vec (map with-id ranks))))))))


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
  (atom {:comments [] :rankings []}))

(defn comment
  [{:keys [winner winner-score loser loser-score] :as c} owner opts]
  (om/component
   (make-table-cols dom/td #js {:className "comment"}
                    [winner winner-score loser-score loser])))

(defn comment-list [{:keys [comments]}]
  (om/component
   (dom/table #js {:className "commentList"}
              (dom/tbody
               nil
               (dom/thead nil (make-table-cols dom/th nil ["winner" "" "" "loser"]))
               (dom/tr nil ;; workaround
                       (make-table-cols dom/td nil ["" "" "" ""]))
               (om/build-all comment (reverse comments))))))

(defn save-comment!
  [comment app opts]
  (do (om/transact! app [:comments]
                  (fn [comments] (conj comments (assoc comment :id (guid)))))
      (go (let [res (<! (http/post (:url opts) {:json-params comment}))]
            (prn (:message res))))))

(defn validate-scores
  "Coerce scores to ints and return if onse is greater than the other

else return [false false]
"
  [score score2]
  (let [score-int (js/parseInt score)
        score2-int (js/parseInt score2)]
    (if (or (> score-int score2-int)
            #_(< score-int score2-int))
      [score-int score2-int]
      [false false])))

(defn handle-submit
  [e app owner opts]
  (let [[winner winner-node] (value-from-node owner "winner")
        [winner-score winner-score-node] (value-from-node owner "winner-score")
        [loser loser-node] (value-from-node owner "loser")
        [loser-score loser-score-node] (value-from-node owner "loser-score")
        [winner-score-int loser-score-int] (validate-scores winner-score loser-score)
        ]
    (when (and winner winner-score-int loser loser-score-int)
      (save-comment! {:winner winner :winner-score winner-score-int
                      :loser loser :loser-score loser-score-int}
                     app opts)
      (clear-nodes! winner-node winner-score-node loser-node loser-score-node))
    false))

(defn comment-form
  [app owner opts]
  (reify
    om/IRender
    (render [_]
      (dom/form
       #js {:className "commentForm" :onSubmit #(handle-submit % app owner opts)}
       (dom/input #js {:type "text" :placeholder "Winner" :ref "winner"})
       (dom/input #js {:type "number"  :placeholder "Score" :ref "winner-score"})
       (dom/input #js {:type "text" :placeholder "Loser" :ref "loser"})
       (dom/input #js {:type "number" :placeholder "Score" :ref "loser-score"})
       (dom/input #js {:type "submit" :value "Post"})))))

(defn comment-box [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (om/transact! app #(assoc % :comments [])))
    om/IWillMount
    (will-mount [_]
      (go (while true
            (fetch-comments app opts)
            (<! (timeout (:poll-interval opts))))))
    om/IRender
    (render [_]
      (dom/div
       #js {:className "commentBox"}
       (dom/h3 nil "Results (most recent first)")
       (om/build comment-form app {:opts opts})
       (om/build comment-list app)
       ))))


(defn ranking
  [{:keys [team ranking rd wins loses suggest] :as fields} owner opts]
  (om/component
   (make-table-cols dom/td nil
                    [team ranking wins loses (.toFixed (/ wins loses) 2) suggest])))

(defn ranking-list [{:keys [rankings]}]
  (om/component
   (dom/table #js {:className "rankingTable"}
              (dom/tbody
               nil
               (dom/thead nil
                          (make-table-cols dom/th nil
                                           ["team" "ranking" "wins" "losses" "w/l" "suggested opponent"]))
               (dom/tr #js {:style #js {:display "none"}} ;; workaround
                       (make-table-cols dom/td nil
                                        ["" "" "" "" "" ""]))
               (om/build-all ranking rankings)
               ))))


(defn rankings-box [app owner opts]
  (reify
    ;; om/IInitState
    ;; (init-state [_]
    ;;   [])
    om/IWillMount
    (will-mount [_]
      (prn "will mount")
      (go (while true
            (fetch-rankings app opts)
            (<! (timeout (:poll-interval opts))))))
    om/IRender
    (render [_]
      (dom/div
       #js {:className "rankingsBox"}
       (dom/h3 nil "Rankings (played more than 2 games)")
       (om/build ranking-list app)
       ))))


(defn tutorial-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "row"}
               (dom/div #js {:className "large-3 columns"
                             :dangerouslySetInnerHTML #js {:__html "&nbsp;"}})
               (dom/div #js {:className "large-6 columns"}
                        (om/build rankings-box app
                                  {:opts {:poll-interval 2000
                                          :url "/rankings"}})
                        (om/build comment-box app
                                  {:opts {:poll-interval 2000
                                          :url "/comments"}}))
               (dom/div #js {:className "large-3 columns"} ""))
      )))

(om/root app-state tutorial-app (.getElementById js/document "content"))
