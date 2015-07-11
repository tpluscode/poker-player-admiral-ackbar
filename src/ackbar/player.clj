(ns ackbar.player
  (:require
   [taoensso.timbre :as log]))

(def version "Admiral Ackbar lives!")

(defn bet-request
  [game-state]
  (log/info (pr-str game-state))
  0)

(defn showdown
  [game-state]
  nil)
