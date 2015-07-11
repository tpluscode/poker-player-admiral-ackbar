#!/usr/bin/env boot

(set-env!
 :source-paths   #{"src" "test"}
 :dependencies
 '[[org.clojure/clojure         "1.7.0"]

   [org.clojure/core.async      "0.1.346.0-17112a-alpha"]
   [org.clojure/core.match      "0.3.0-alpha4"]
   [org.clojure/core.incubator  "0.1.3"]
   [org.clojure/data.generators "0.1.2"]

   [environ                     "1.0.0"]
   [danielsz/boot-environ       "0.0.4"]
   [org.danielsz/system         "0.1.7"]
   [org.clojure/tools.nrepl     "0.2.10"]

   [ring                        "1.3.2"]
   [ring/ring-defaults          "0.1.4"]
   [http-kit                    "2.1.19"]
   [compojure                   "1.3.3"]
   [clj-json                    "0.5.3"]
   [com.taoensso/timbre         "3.3.1"]

   [adzerk/boot-test            "1.0.4" :scope "test"]
   ])

(require
 '[reloaded.repl              :refer [init start stop go reset]]
 '[ackbar.systems             :refer [dev-system]]
 '[danielsz.boot-environ      :refer [environ]]
 '[system.boot                :refer [system]]
 '[adzerk.boot-test           :refer [test]])

(deftask dev
  "Run a restartable system in the Repl"
  []
  (comp
   (environ :env {:http-port 3000})
   (watch :verbose true)
   (system :sys #'dev-system :auto-start true)
   (repl :server true)))

(deftask r
  "Run a Clojure repl"
  []
  (comp
    (repl :client true)))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot :namespace '#{ackbar.core})
   (pom :project 'ackbar
        :version "1.0.0")
   (uber)
   (jar :main 'ackbar.core
        :file "ackbar-standalone.jar")))
