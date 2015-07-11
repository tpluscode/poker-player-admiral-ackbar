(ns ackbar.handler
  (:require
   [ackbar.player :as player]
   [ring.middleware.params :refer [wrap-params]]
   [clj-json.core :as json]
   [taoensso.timbre :as log]))

(defn game-state [req]
  (try
    (-> (get-in req [:params "game_state"])
        (json/parse-string true))
    (catch Exception ex
      (log/error "Bad JSON" ex)
      {})))

(defn handler [req]
  (case (-> req :params (get "action"))
    "bet_request"
    (let [bet (player/bet-request (game-state req))]
      (log/info "Bet" bet)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (str bet)})

    "showdown"
    (let [showdown (player/showdown (game-state req))]
      (log/info "Showdown" showdown)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (str showdown)})

    "version"
    (do
      (log/info "Version" player/version)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (str player/version)})

    "check"
    (do
      (log/info "Check" player/version)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    ""})

    (do
      (log/warn "Bad request" req)
      {:status 400})))

(def app
  (wrap-params handler))

