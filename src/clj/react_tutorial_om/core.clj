(ns react-tutorial-om.core
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as resp]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [ranking-algorithms.core :as rank]
            ))

(def results (atom [{:winner "chris", :winner-score 10, :loser "losers", :loser-score 0}
                    {:winner "arsenal", :winner-score 3, :loser "chelsea", :loser-score 0}]))

(def db-file "results.json")

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn load-json-file [file]
  (-> (slurp file)
      (json/parse-string true)
      vec))

(defn init
  []
  (reset! results (load-json-file db-file)))

(defn save-match! ;; TODO: write to a db
  [{:keys [body]}]
  (let [comment (-> body io/reader slurp (json/parse-string true)
                    ;; TODO: coerce data earlier
                    (update-in [:winner] clojure.string/lower-case)
                    (update-in [:loser] clojure.string/lower-case)
                    (update-in [:date] (fn [_] (java.util.Date.))))]
    (swap! results conj comment)
    (spit db-file (json/generate-string @results)) ;; put in channel?
    (json-response
     {:message "Saved comment!"})))

(defn translate-keys [{:keys [winner winner-score loser loser-score]}]
  {:home winner
   :home_score winner-score
   :away loser
   :away_score loser-score})

(defn calc-ranking-data [matches]
  (map-indexed
   (partial rank/format-for-printing matches)
   (rank/top-teams 30 matches)
   #_(do (prn (rank/top-glicko-teams 30 matches))
         (rank/top-glicko-teams 30 matches {}))))

(defn attach-player-matches [results rankings]
  (for [rank rankings]
    (assoc-in rank [:matches] (rank/show-matches (:team rank) results))))

(defn normalise-indexes
  "Move out of bounds indexes into the next free position

  10 2 [-2 -1 1 2] => (4 3 1 2)
  10 2 [7 8 10 11] => (7 8 5 6)"
  [total offset idxs]
  (for [idx idxs]
    (cond
     (neg? idx) (- offset idx)
     (>= (inc idx) total) (- (dec idx) offset offset)
     :else idx)))

(defn suggest-opponent
  "Given a user match history and map of user ranks suggest the next
  oppenent a user should face. Ranks is a vector of people.

  TODO: tidy up"
  [{:keys [rank matches]} ranks]
  (try
    (let [offset 2
          idx-rank (dec rank)
          opps1 (range rank (+ rank offset))
          opps2 (range (- idx-rank offset) idx-rank)
          allopps  (->> (concat opps1 opps2)
                        (normalise-indexes (count ranks) offset))
          oppnames-set (set (map ranks allopps))
          matchfreqs (frequencies (filter #(contains? oppnames-set %)
                                          (map :opposition matches)))
          near-totals (reduce (fn [acc [k v]] (assoc acc k v))
                              (zipmap oppnames-set (repeat 0)) ;; Start at 0
                              matchfreqs)
          sorted-totals (sort-by second near-totals)
          ]
       (ffirst sorted-totals))
    (catch java.lang.IndexOutOfBoundsException e
      (prn "Index error: " rank (str e))
      (str e))))

(defn- vectorise-names [rankings]
  (vec (map :team rankings)))

(defn attach-suggested-opponents
  [rankings]
  (let [vec-ranks (vectorise-names rankings)]
    (for [rank rankings]
      (assoc-in rank [:suggest] (suggest-opponent rank vec-ranks)))))

(defn attach-uniques [rankings]
  (for [rank rankings]
    (->> rank
         :matches
         (filter (fn [x] (> (:for x) (:against x))))
         (map :opposition)
         (into #{})
         count
         (assoc-in rank [:u-wins]))))

(defn unique-players [results]
  (into (into #{} (map :home results))
       (map :away results)))

(defroutes app-routes
  (GET "/" [] (resp/redirect "/index.html"))
  (GET "/init" [] (init) "inited")
  (GET "/matches" [] (json-response
                       {:message "Here's the results!"
                        :matches @results}))
  (POST "/matches" req (save-match! req))

  (GET "/rankings" []
       (let [-results (map translate-keys @results)]
         (json-response
          {:message "Some rankings"
           :players (unique-players -results)
           :rankings  (->> (calc-ranking-data -results)
                           (attach-player-matches -results)
                           attach-suggested-opponents
                           attach-uniques
                           (filter (fn [{:keys [loses wins]}] (> (+ loses wins) 2))))})))

  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> #'app-routes
      (handler/api)))

;; (init)
(comment
  "
 TODO:
* use a db => datomic?
* sort by col
* click on player -> match history
* bbc football style last 5 games component, hover
* proper glicko
* generic sortable table component
* unique wins, etc - some kind of distribution concept, who is most rounded
* can only play against people near you (+/- 3). how to handle top people?
* date in tooltip
 ")
