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
    (println "Failed: " (:failed-count data)
             "\n"
             "Passed: " (:passed-count data))))


(defn describe [s]
  (println s))

(defn it [s]
  (print "\t" s ": "))

(defn e [it-str clause expected actual]
  (let [passed (atom true)]
    (it it-str)
    (try
      (expect expected clause actual)
      (catch js/Error err
        (let [m (.-message err)]
          (println "FAIL\n" m)
          (failed!)
          (swap! passed not)))
      (finally
        (when @passed
          (passed!)
          (println "PASS"))))))
    
(set! *main-cli-fn*
      
      (fn [& _]

        (describe "PossibleDB RethinkDB Table Prefix")
        
        (e "possibledb-table-name fn"
           :to.equal
           (str pdb/possibledb-rethinkdb-prefix
                "example_db")
           (pdb/possibledb-table-name "example-db"))

        (results)))


