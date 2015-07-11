(ns ackbar.systems
  (:require
   [ackbar.handler :refer [app]]
   [environ.core :refer [env]]
   [system.core :refer [defsystem]]
   (system.components
    [http-kit :refer [new-web-server]]
    [repl-server :refer [new-repl-server]])))

(defsystem dev-system
  [:web (new-web-server (Integer. (env :http-port)) app)])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port)) app)
   ; :repl-server (new-repl-server (Integer. (env :repl-port)))
   ])
