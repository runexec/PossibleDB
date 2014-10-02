(ns test
  (:require
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

        (results)))


