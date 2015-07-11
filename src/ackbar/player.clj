(ns ackbar.player
  (:require
   [taoensso.timbre :as log]))

(def version "Admiral Ackbar lives!")

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

(defn capped [value state]
  (let [p (admiral state)
        stack (max (:stack p) 500)
        bet (+ value (:bet p))
        limit (/ stack 2)]
    (if (> bet limit)
      0
      bet)))


(defn bet-request
  [game-state]
  (log/info (pr-str game-state))
  (let [[a b] (map :rank (hole-cards game-state))
        small-bet (capped (small-raise game-state) game-state)
        large-bet (capped (* 2 small-bet) game-state)
        check-bet (check game-state)
        naive-hand-type (eval-hand (take 5 (concat (hole-cards game-state)
                                                   (community-cards game-state))))
        super-naive-hand-type (eval-hand (concat (hole-cards game-state)
                                                 (community-cards game-state)))
        community-hand-type (eval-hand (community-cards game-state))]
    (log/info "[a, b]: [%s %s]" a b)
    (cond
      (and (#{:straight-flush} naive-hand-type)
           (not= naive-hand-type :flop)
           (not= naive-hand-type community-hand-type))
      (log/spy :info :straight-flush-all-in all-in)

      (and (#{:four-of-a-kind} naive-hand-type)
           (not= naive-hand-type :flop)
           (= a b)
           (not= naive-hand-type community-hand-type))
      (log/spy :info :four-of-a-kind-all-in all-in)

      (and (#{:straight-flush :four-of-a-kind :full-house :three-of-a-kind}
            naive-hand-type)
           (not= naive-hand-type :flop)
           (not= naive-hand-type community-hand-type))
      (log/spy :info :good-large-bet large-bet)

      ;(and (#{:flush :straight :two-pair :pair} super-naive-hand-type)
      (and (not= :flop super-naive-hand-type)
           (not= naive-hand-type community-hand-type))
      (log/spy :info :kinda-small-bet check-bet)

      (and (> a 9) (> b 9) (= naive-hand-type :flop))
      (log/spy :info :flop-large-bet check-bet)

      (and (or (> a 9) (> b 9)) (= naive-hand-type :flop))
      (log/spy :info :flop-small-bet check-bet)

      (and (= a b) (= naive-hand-type :flop))
      (log/spy :info :flop-small-pair check-bet)

      :else (log/spy :info :fold 0))))

(defn showdown
  [game-state]
  nil)

