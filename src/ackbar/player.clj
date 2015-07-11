(ns ackbar.player
  (:require
   [taoensso.timbre :as log]))

(def version "Admiral Ackbar lives!")

(defn admiral [state]
  (-> state
      (:players)
      (nth (:in_action state))))

(defn hole-cards [state]
  (let [p (admiral state)]
    (->> p
         (:hole_cards)
         (map :rank)
         (map (fn [c]
                (case c
                   "A" 14
                   "K" 13
                   "Q" 12
                   "J" 11
                   (Integer/parseInt c)))))))

(defn small-raise [state]
  (let [admiral (admiral state)
        current-buy-in (:current_buy_in state)
        current-bet (:bet admiral)
        min-raise (:minimum_raise state)]
    (- current-buy-in current-bet (- min-raise))))

(defn bet-request
  [game-state]
  (log/info (pr-str game-state))
  (let [[a b] (hole-cards game-state)
        bet (small-raise state)]
    (if (and (> a 9) (> b 9))
      bet
      0)))

(defn showdown
  [game-state]
  nil)

