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
  (-> (slurp db-file)
      (json/parse-string true)
      vec))

(defn init
  []
  (reset! results (load-json-file db-file)))

(defn save-comment! ;; TODO: write to a db
  [{:keys [body]}]
  (let [comment (-> body io/reader slurp (json/parse-string true)
                    ;; TODO: coerce data earlier
                    (update-in [:winner] clojure.string/lower-case)
                    (update-in [:loser] clojure.string/lower-case))]
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
  (let [data (map translate-keys matches)]
    (map-indexed
     (partial rank/format-for-printing data)
     (do (prn (rank/top-glicko-teams 15 data))
         (rank/top-glicko-teams 30 data {})))))

(defroutes app-routes
  (GET "/" [] (resp/redirect "/index.html"))
  (GET "/init" [] (init) "inited")
  (GET "/comments" [] (json-response
                       {:message "Here's the results!"
                        :comments @results}))
  (POST "/comments" req (save-comment! req))

  (GET "/rankings" []
       (json-response
        {:message "Some rankinsg"
         :rankings  (filter (fn [{:keys [loses wins]}] (> (+ loses wins) 2))
                            (calc-ranking-data @results))}))



  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> #'app-routes
      (handler/api)))

(init)
;; TODO:
(comment
  "
* calc ranks
* ranks endpoint
* ranks component
* date
* css
* use a db
 ")
