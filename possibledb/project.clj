
(defproject possibledb "0.1-6"
  :description "PossibleDB is a Datomic Database Server clone built with DataScript, RethinkDB, Clojure, ClojureScript, and NodeJS."
  :url "http://github.com/runexec/PossibleDB"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 
                 ;; [com.cognitect/transit-cljs "0.8.188"]
                 
                 [datascript "0.4.1"]
                 [org.bodil/cljs-noderepl "0.1.11"]
                 [com.cemerick/piggieback "0.1.3"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  ;;
  ;; user> (cemerick.piggieback/cljs-repl)
  ;; 
  ;; or
  ;;
  ;; user=> (require '[cljs.repl.node :as node])
  ;; user=> (node/run-node-nrepl)
  ;; 
  
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  
  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "strap"
              :source-paths ["src"]
              :compiler {:output-to "possibledb.js"
                         :output-dir "out"
                         :target :nodejs
                         ;; :source-map "possibledb.js.map"
                         :optimizations :simple}}]})
