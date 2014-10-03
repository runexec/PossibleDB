(ns test
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [put! chan <! timeout]]
   [cljs.nodejs :as node]
   [possibledb.core :as pdb]
   ;; nodejs chai
   [latte.chai :refer [expect]]))


(node/enable-util-print!)

(def tests
  (atom {:failed-count 0
         :passed-count 0}))

(defn failed! []
  (swap! tests
         update-in
         [:failed-count] inc))

(defn passed! []
  (swap! tests
         update-in
         [:passed-count] inc))

(defn results []
  (let [data @tests]
    (println
     "\n"
     (repeat 20 "=")
     "\nFailed: " (:failed-count data)
     "\nPassed: " (:passed-count data))))


(defn describe [s]
  (println s))

(defn it [s]
  (println s))

(defn e [it-str clause expected actual]
  (let [passed (atom true)
        msg (atom false)]
    (try
      (expect expected clause actual)

      (catch js/Error err
        (let [m (.-message err)]
          (failed!)
          (print "\tFAIL: ")
          (swap! passed not)
          (reset! msg m)))

      (finally
        (when @passed
          (passed!)
          (print "\tPASS: "))
        
        (it it-str)
        
        (if-let [m @msg]
          (println "\t\t" m))))))


(set! *main-cli-fn*
      
      (fn [& _]

        ;; go-block needed for parking
        (go

          (describe "PossibleDB RethinkDB Table Prefix")
          
          (e "possibledb-table-name fn"
             :to.equal
             (str pdb/possibledb-rethinkdb-prefix
                  "example_db")
             (pdb/possibledb-table-name "example-db"))


          (e "possibledb-db-name fn"
             :to.equal
             (str pdb/possibledb-db-prefix
                  "example_db")
             (pdb/possibledb-db-name "example-db"))

          (describe "JSON Conversion Helper")

          (e "clj->json fn"
             :to.eql
             (JSON/parse "{\"a\":1}")
             (pdb/clj->json {:a 1}))

          (describe "Configuration Loading")
          
          (e "load-config! fn"
             :to.equal
             false
             (nil?
              (pdb/load-config! pdb/config-file)))


          (put! pdb/ch-main
                {:load-config pdb/config-file})
          
          ;; waiting
          
          (<! pdb/ch-ready)
                      
          (e "sub pub-main :load-config"
             :to.equal
             false
             (nil?
              (:rethinkdb-config @pdb/app)))

          (e "rethinkdb-config [] fn"
             :to.equal
             false
             (nil?
              (pdb/rethinkdb-config)))

          (e "rethinkdb-config [attr] fn"
             :to.equal
             false
             (let [x (pdb/rethinkdb-config "host")]
               (or (empty? x)
                   (nil? x))))
          
          (describe "RethinkDB Connection Handlers")

          (e "rethinkdb-conn-set!"
             :to.eql
             :xyz
             (:rethinkdb-conn
              (pdb/rethinkdb-conn-set! :xyz)))

          (e "rethinkdb-conn"
             :to.eql
             :xyz
             (pdb/rethinkdb-conn))

          (e "rethinkdb-safe-error?"
             :to.equal
             "already exists"
             (pdb/rethinkdb-safe-error? "Table-or-something already exists"))
              

          ;; TODO
          
          (try
            (-> @pdb/app
                :rethinkdb-conn
                (.close #js {:noreplyWait false}
                        (fn [& _]
                          ;; close handler
                          )))
            (catch js/Error ex ex))
          
          (results))))


