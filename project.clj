 (defproject nubank-challenge "0.1.0-SNAPSHOT"
   :description "NuBank Coding Challenge - Robots and dinosaurs REST API."
   :dependencies [[org.clojure/clojure "1.8.0"]
                  [metosin/compojure-api "1.1.11"]]
   :ring {:handler nubank-challenge.handler/app}
   :uberjar-name "server.jar"
   :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [cheshire "5.8.1"]
                                  [ring/ring-mock "0.4.0"]
                                  [midje "1.8.3"]]
                   :plugins [[lein-ring "0.12.0"]
                             [lein-midje "3.2"]]}})
