(ns react-tutorial-om.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   ;; #_[secretary.macros :refer [defroute]]
                   [react-tutorial-om.utils :refer [logm]]
                   )
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan timeout]]
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
  (atom {:matches [] :rankings [] :players [] :conn? true
         :player-view {:display false :player nil} }))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn comment
  [{:keys [winner winner-score loser loser-score date]} owner opts]
  (om/component
   (apply dom/tr #js {:className "comment"}
          (map #(dom/td nil %) [winner winner-score loser-score loser]))))

(defn comment-list [{:keys [matches]}]
  (om/component
   (dom/table #js {:className "commentList"}
              (dom/thead nil (apply dom/tr nil
                                    (map #(dom/th nil %) ["winner" "" "" "loser"])))
              (apply
               dom/tbody nil
               (om/build-all comment (take 20 (reverse matches)))))))

(defn save-match!
  [match app opts]
  (do (om/transact! app [:matches]
                  (fn [matches] (conj matches match)))
      (go (let [res (<! (http/post (:url opts) {:edn-params match}))]
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
    ;; om/IInitState
    ;; (init-state [_]  ;; this errors on 0.6 and doesn't seem needed
    ;;   (om/transact! app #(assoc % :matches [])))
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
   (apply dom/ul #js {:className "last-10-games" :style #js {:width "130px"}}
          (map #(let [win? (> (:for %) (:against %))]
                  (dom/li #js {:className (str (if win? "win" "loss") " hover")}
                          (dom/span nil (if win? "Win" "Loss"))
                          (dom/div #js {:className "atooltip"}
                                   (str (if win? "Win " "Loss ")
                                        (:for %) " - " (:against %)
                                        " against " (:opposition %)
                                        " @ " (:date %)))))
               (->> results
                    (take-last 10)
                    )))))

(defn player-summary
  [{{:keys [team ranking rd wins loses suggest matches] :as fields} :data
    show :display} owner opts]
  (om/component
   (dom/div #js {:style (display show)}
            (dom/h4 nil "Player Stats")
            (dom/ul #js {:className "pricing-table"}
                    (dom/li #js {:className "title"} team)
                    (dom/li #js {:className "price"} ranking)
                    (dom/li #js {:className "bullet-item"} (str  wins " - " loses))
                    (apply dom/li #js {:className "bullet-item"}
                           (map (fn [[name matches*]]
                                  (let [wins (reduce (fn [tot {:keys [for against]}]
                                                       (if (> for against)
                                                         (inc tot)
                                                         tot)) 0 matches*)
                                        losses (- (count matches*) wins)]
                                    (dom/li
                                     nil
                                     (dom/div #js {:className "row"}
                                              (dom/div #js {:className "small-6 columns"}
                                                       (str name ": " wins "-" losses)
                                                       (dom/div #js {:className "progress success"}
                                                                (dom/span
                                                                 #js {:className "meter"
                                                                      :style #js {:width (str (Math/round
                                                                                               (* 100 (/ wins (+ wins losses)))) "%")}})))
                                              (dom/div #js {:className "small-6 columns"}
                                                       (om/build last-10-games (take-last 5 matches*)))))))
                                (reverse (sort-by (fn [[_ games]] (count games))
                                                  (group-by :opposition matches)))))))))

(defn ranking
  [{:keys [team rank ranking rd wins loses suggest] :as fields} owner opts]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-player-ch]}]
      (apply dom/tr nil
             (map #(dom/td nil %)
                  [rank
                   (dom/span #js {:onClick (fn [e] (put! select-player-ch team))
                                  :style #js {:cursor "pointer"}}
                             team)
                   ranking wins loses (.toFixed (/ wins loses) 2) suggest
                   (om/build last-10-games (:matches fields))])))))

(defn ranking-list [rankings owner opts]
  (om/component
   (dom/table #js {:className "rankingTable"}
              (dom/thead nil
                          (apply dom/tr nil
                                 (map #(dom/th nil %)
                                      ["" "team" "ranking" "w" "l" "w/l"
                                       "suggested opponent" "last 10 games"])))
              (apply
               dom/tbody nil
               (om/build-all
                ranking
                rankings
                {:init-state {:select-player-ch (:select-player-ch opts)}})))))


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
       (om/build ranking-list (:rankings app) {:opts opts})))
    ))

(defn status-box [conn? owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "alert-box warning radius"
                    :style (display (not conn?))}
               "Connection problem!"))))

(defn ladder-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:select-player-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [select-player-ch (om/get-state owner :select-player-ch)]
        (go (loop []
              (let [player (<! select-player-ch)]
                (om/transact!
                 app :player-view
                 #(-> %
                      ((fn [x]
                         (if (= (:player x)
                                player)  ;; toggle same player
                           (assoc x :display (not (:display x)))
                           (assoc x :display true))))
                      (assoc :player player))))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [select-player-ch]}]
      (dom/div #js {:className "row results-row"}
               (dom/div #js {:className "large-2 columns"
                             :dangerouslySetInnerHTML #js {:__html "&nbsp;"}})
               (dom/div #js {:className "large-7 columns"}
                        (om/build status-box (:conn? app))
                        (om/build rankings-box app
                                  {:opts {:poll-interval 2000
                                          :url "/rankings.edn"
                                          :select-player-ch select-player-ch}})
                        (om/build comment-box app
                                  {:opts {:poll-interval 2000
                                          :url "/matches.edn"}}))
               (dom/div #js {:className "large-3 columns"}
                        (om/build
                         player-summary
                         {:data  (first
                                  (filter #(= (:team %)
                                              (get-in app [:player-view :player]))
                                          (:rankings app)))
                          :display (get-in app [:player-view :display])})))
      )))

(om/root ladder-app
         app-state
         {:target (.getElementById js/document "content")})
