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
        small-bet (small-raise game-state)
        large-bet (* 2 small-bet)]
    (log/info "[a, b]: [%s %s]" a b)
    (cond
      (and (> a 9) (> b 9)) (log/spy :info :large-bet large-bet)
      (or (> a 9) (> b 9)) (log/spy :info :small-bet small-bet)
      (or (= a b)) (log/spy :info :small-bet-pair small-bet)
      :else (log/spy :info :fold 0))))

(defn showdown
  [game-state]
  nil)



; (defn make-card [[s r]]
;   {:suit ({\D :diamond \H :heart \C :club \S :spade} s)
;    :rank (.indexOf (seq "23456789TJQKA") r)})

; (defn eval-hand [hand]
;     (let [hand (map (fn [[s r]] [s (.indexOf (seq "23456789TJQKA") r)]) hand)
;           ranks (map last hand)
;           flush (apply = (map first hand))
;           straight? #(= (sort %) (map (partial + (apply min %)) (range 5)))
;           straight (or (straight? ranks) (straight? (replace {12 -1} ranks)))
;           freq (sort (vals (frequencies ranks)))]
;       (cond
;         (and flush straight) :straight-flush
;         flush :flush
;         straight :straight
;         (= freq [1 4]) :four-of-a-kind
;         (= freq [2 3]) :full-house
;         (= freq [1 1 3]) :three-of-a-kind
;         (= freq [1 2 2]) :two-pair
;         (= freq [1 1 1 2]) :pair
;         :else :high-card)))
