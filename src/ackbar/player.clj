(ns ackbar.player
  (:require
   [clojure.math.combinatorics :as combo]
   [taoensso.timbre :as log]))

(def version "Admiral Ackbar p4wns your bot!")

(defn admiral [state]
  (-> state
      (:players)
      (nth (:in_action state))))

(defn make-card [{:keys [:rank :suit]}]
  {:rank (case rank "A" 14 "K" 13 "Q" 12 "J" 11 (Integer/parseInt rank))
   :suit suit})

(defn eval-hand [hand]
  (if (< (count hand) 5)
    :flop
    (let [hand (map (fn [hand] [(:suit hand) (:rank hand)]) hand)
          ranks (map last hand)
          flush (apply = (map first hand))
          straight? #(= (sort %) (map (partial + (apply min %)) (range 5)))
          straight (or (straight? ranks) (straight? (replace {12 -1} ranks)))
          freq (sort (vals (frequencies ranks)))]
      (cond
        (and flush straight) :straight-flush
        flush :flush
        straight :straight
        (= freq [1 4]) :four-of-a-kind
        (= freq [2 3]) :full-house
        (= freq [1 1 3]) :three-of-a-kind
        (= freq [1 2 2]) :two-pair
        (= freq [1 1 1 2]) :pair
        :else :high-card))))

(def hand-ranks
  {:straight-flush 10
   :four-of-a-kind 9
   :full-house 8
   :flush 7
   :straight 6
   :three-of-a-kind 5
   :two-pair 4
   :pair 3
   :high-card 2
   :flop 1})

(defn eval-7-hand [hand]
  (->> (combo/combinations hand 5)
       (map eval-hand)
       (map (fn [x] [(hand-ranks x) x]))
       (concat [[-1 :flop]])
       (sort)
       (last)
       (last)))

(defn hole-cards [state]
  (let [p (admiral state)]
    (->> p
         (:hole_cards)
         (map make-card))))

(defn community-cards [state]
  (->> state
       :community_cards
       (map make-card)))

(defn small-raise [state]
  (let [admiral (admiral state)
        current-buy-in (:current_buy_in state)
        current-bet (:bet admiral)
        min-raise (:minimum_raise state)]
    (- current-buy-in current-bet (- min-raise))))

(defn check [state]
  (let [admiral (admiral state)
        current-buy-in (:current_buy_in state)
        current-bet (:bet admiral)]
    (- current-buy-in current-bet)))

(defn all-in [state]
  (:stack (admiral state)))

(defn capped [value state ratio]
  (let [p (admiral state)
        stack (max (:stack p) 500)
        bet (+ value (:bet p))
        limit (/ stack ratio)]
    bet))
    ; (if (and (> bet limit) (or (< (:round state) 40) (> (count (:players state)) 3)))
    ;   limit
    ;   bet)))

(defn bet-request
  [game-state]
  (log/info (pr-str game-state))
  (let [[a b] (map :rank (hole-cards game-state))
        player (admiral game-state)
        small-bet (capped (small-raise game-state) game-state 2)
        large-bet (capped (* 2 small-bet) game-state 2)
        check-bet (check game-state)
        hand-type (eval-7-hand (concat (hole-cards game-state)
                                       (community-cards game-state)))
        community-hand-type (eval-hand (community-cards game-state))]
    (log/info "[a, b]: [%s %s]" a b)
    (cond
      (and (#{:straight-flush} hand-type)
           (not= hand-type :flop)
           (not= hand-type community-hand-type))
      (log/spy :info :straight-flush-all-in all-in)

      (and (#{:four-of-a-kind} hand-type)
           (not= hand-type :flop)
           (= a b)
           (not= hand-type community-hand-type))
      (log/spy :info :four-of-a-kind-all-in all-in)

      (and (#{:straight-flush :four-of-a-kind :full-house :three-of-a-kind}
            hand-type)
           (not= hand-type :flop)
           (not= hand-type community-hand-type))
      (log/spy :info :good-large-bet large-bet)

      (and (not= :flop hand-type))
      (log/spy :info :kinda-small-bet check-bet)

      (and (= hand-type :flop) (= a b))
      (log/spy :info :flop-small-pair small-bet)

      (and (> a 9) (> b 9) (= hand-type :flop) (> (capped check-bet game-state 2) 0))
      (log/spy :info :flop-large-bet (capped check-bet game-state 2))

      ; (and (or (> a 9) (> b 9)) (= hand-type :flop))
      ; (log/spy :info :flop-small-bet (capped check-bet game-state 4))

      (and (= hand-type :flop) (> (:bet player) 0) (< check-bet (/ (:stack player) 2)))
      (log/spy :info :flop-just-checking (capped check-bet game-state 2))

      (and (> (:bet player) 0))
      (log/spy :info :always-checking (capped large-bet game-state 2))

      :else (log/spy :info :fold 0))))

(defn showdown
  [game-state]
  nil)

