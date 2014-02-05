(ns react-tutorial-om.core
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.response :as resp]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(def comments (atom []))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn init
  []
  (reset! comments (-> (slurp "comments.json")
                       (json/parse-string true)
                       vec)))

(defn save-comment!
  [{:keys [body]}]
  (let [comment (-> body io/reader slurp (json/parse-string true))]
    (swap! comments conj comment)
    (json-response
     {:message "Saved comment!"})))

(defroutes app-routes
  (GET "/" [] (resp/redirect "/index.html"))

  (GET "/comments" [] (json-response
                       {:message "Here's the comments!"
                        :comments @comments}))
  
  (POST "/comments" req (save-comment! req))

  (route/resources "/")
  
  (route/not-found "Page not found"))

(def app
  (-> #'app-routes
      handler/api))
