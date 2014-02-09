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

(defn- fetch-matches
  "The comments need to be a vector, not a list. Not sure why."
  [app opts]
  (go (let [{{matches :matches} :body} (<! (http/get (:url opts)))]
        (when matches
          (om/transact!
           app #(assoc % :matches matches))))))

(defn- fetch-rankings
  "The comments need to be a vector, not a list. Not sure why."
  [app opts]
  (go (let [{{:keys [rankings players]}
             :body status :status} (<! (http/get (:url opts)))]
        (if rankings
          (om/transact!
           app #(-> %
                    (assoc :rankings rankings)
                    (assoc :players players)
                    (assoc :conn? true)))
          (om/transact!
           app #(assoc % :conn? false))))))


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
  (atom {:matches [] :rankings [] :players [] :conn? true}))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn comment
  [{:keys [winner winner-score loser loser-score date]} owner opts]
  (om/component
   (make-table-cols dom/td #js {:className "comment"}
                    [winner winner-score loser-score loser])))

(defn comment-list [{:keys [matches]}]
  (om/component
   (dom/table #js {:className "commentList"}
              (dom/tbody
               nil
               (dom/thead nil (make-table-cols dom/th nil ["winner" "" "" "loser"]))
               (dom/tr nil ;; workaround
                       (make-table-cols dom/td nil ["" "" "" ""]))
               (om/build-all comment (take 20 (reverse matches)))))))

(defn save-match!
  [match app opts]
  (do (om/transact! app [:matches]
                  (fn [matches] (conj matches match)))
      (go (let [res (<! (http/post (:url opts) {:json-params match}))]
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
  [e app owner opts {:keys [winner winner-score loser loser-score]}]
  (let [winner (clojure.string/trim winner)
        winner-score (clojure.string/trim winner-score)
        loser (clojure.string/trim loser)
        loser-score (clojure.string/trim loser-score)
        [winner-score-int loser-score-int] (validate-scores winner-score loser-score)]
    (when (and winner winner-score-int loser loser-score-int)
      (save-match! {:winner winner :winner-score winner-score-int
                      :loser loser :loser-score loser-score-int}
                     app opts)
      (doseq [key [:winner :winner-score :loser :loser-score]]
        (om/set-state! owner key "")))
    false))

(defn handle-change [e owner key]
  (om/set-state! owner key (.. e -target -value)))

(defn comment-form
  [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:winner "" :winner-score ""
       :loser "" :loser-score ""})
    om/IRenderState
    (render-state [this state]
      (dom/form
       #js {:className "commentForm" :onSubmit #(handle-submit % app owner opts state)}
       (dom/input #js {:type "text" :placeholder "Winner" :ref "winner"
                       :value (:winner state) :list "players"
                       :onChange #(handle-change % owner :winner)})
       (dom/input #js {:type "number"  :placeholder "Score" :ref "winner-score"
                       :value (:winner-score state)
                       :onChange #(handle-change % owner :winner-score)})
       (dom/input #js {:type "text" :placeholder "Loser" :ref "loser"
                       :value (:loser state) :list "players"
                       :onChange #(handle-change % owner :loser)})
       (dom/input #js {:type "number" :placeholder "Score" :ref "loser-score"
                       :value (:loser-score state)
                       :onChange #(handle-change % owner :loser-score)})
       (dom/input #js {:type "submit" :value "Post"})
       (apply dom/datalist #js {:id "players"}
              (map #(dom/option #js {:value %})
                   (:players app)))))))

(defn comment-box [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (om/transact! app #(assoc % :matches [])))
    om/IWillMount
    (will-mount [_]
      (go (while true
            (fetch-matches app opts)
            (<! (timeout (:poll-interval opts))))))
    om/IRender
    (render [_]
      (dom/div
       #js {:className "commentBox"}
       (dom/h3 nil "Results (most recent first)")
       (om/build comment-form app {:opts opts})
       (om/build comment-list app)
       ))))

(defn last-10-games [results owner]
  (om/component
   (apply dom/ul #js {:className "last-10-games"}
          (map #(let [win? (> (:for %) (:against %))]
                  (dom/li #js {:className (str (if win? "win" "loss") " hover")}
                          (dom/span nil (if win? "Win" "Loss"))
                          (dom/div #js {:className "atooltip"}
                                   (str (if win? "Win " "Loss ")
                                        (:for %) " - " (:against %)
                                        " against " (:opposition %)))))
               (->> results
                    (take-last 10)
                    )))))

(defn ranking
  [{:keys [team ranking rd wins loses suggest] :as fields} owner opts]
  (om/component
   (make-table-cols dom/td nil
                    [team ranking wins loses (.toFixed (/ wins loses) 2) suggest
                     (om/build last-10-games (:matches fields))])))

(defn ranking-list [rankings]
  (om/component
   (dom/table #js {:className "rankingTable"}
              (dom/tbody
               nil
               (dom/thead nil
                          (make-table-cols dom/th nil
                                           ["team" "ranking" "wins" "losses" "w/l" "suggested opponent" "last 10 games"]))
               (dom/tr #js {:style #js {:display "none"}} ;; workaround
                       (make-table-cols dom/td nil
                                        ["" "" "" "" "" "" ""]))
               (om/build-all ranking rankings)
               ))))


(defn rankings-box [app owner opts]
  (reify
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
       (om/build ranking-list (:rankings app))))
    om/IShouldUpdate
    (should-update [this next-props next-state]
      (not= (:rankings next-props)
            (:rankings app)))
    ))

(defn status-box [conn? owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "alert-box warning radius"
                    :style (display (not conn?))}
               "Connection problem!"))))

(defn tutorial-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "row results-row"}
               (dom/div #js {:className "large-3 columns"
                             :dangerouslySetInnerHTML #js {:__html "&nbsp;"}})
               (dom/div #js {:className "large-6 columns"}
                        (om/build status-box (:conn? app))
                        (om/build rankings-box app
                                  {:opts {:poll-interval 2000
                                          :url "/rankings"}})
                        (om/build comment-box app
                                  {:opts {:poll-interval 2000
                                          :url "/matches"}}))
               (dom/div #js {:className "large-3 columns"} ""))
      )))

(om/root app-state tutorial-app (.getElementById js/document "content"))
