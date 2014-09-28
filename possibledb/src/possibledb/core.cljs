(ns possibledb.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as s]
   [cljs.reader :as reader]
   [cljs.nodejs :as node]
   [cljs.core.async :refer [put! >! <! pub sub unsub chan timeout]]
   [datascript :as d]
   [datascript.core :as dc]))


(set! *main-cli-fn*

      (fn [& [port]]
        
        (node/enable-util-print!)

        ;; needed for reading config files
        (def fs (node/require "fs"))

        ;; rethinkdb backs everything
        (def r (node/require "rethinkdb"))

        ;; net socket for requests
        (def net (node/require "net"))

        ;; altered, but not added kv pairs
        (def app
          (atom
           {:possibledb-table "databases"
            :config-file "possibledb.config"
            :reathinkdb-config nil
            :reathinkdb-conn nil}))

        ;; each db is actually a single row in a single table
        ;; and this is the prefix for the main table
        (def possibledb-rethinkdb-prefix "possibledb_")

        (defn possibledb-table-name [name]
          (s/replace (str possibledb-rethinkdb-prefix name)
                     #"-"
                     "_"))

        ;; prefix for all non-main tables within the db
        (def possibledb-db-prefix "possibledb_db_")

        (defn possibledb-db-name [name]
          (s/replace (str possibledb-db-prefix name)
                     #"-"
                     "_"))

        ;; returns main table
        (defn rethinkdb-possibledb-table []
          (-> @app :possibledb-table possibledb-table-name))

        ;; clj->js has a bug for datoms => workaround is used later
        (defn clj->json [x]
          (-> x
              clj->js
              JSON/stringify
              JSON/parse))

        ;; used when the main-fn is ready to run
        (def ch-ready (chan))

        ;; owned by pub-main
        (def ch-main (chan))

        ;; pub fn is key of single key hashmaps 
        (def pub-main (pub ch-main (comp key first)))

        (def config-file (:config-file @app))

        (defn load-config! [config-file]
          "Use aget to get val of key"
          (let [data (.readFileSync fs
                                    config-file
                                    #js {:encoding "utf8"})]
            (JSON/parse data)))

        ;; load config
        (let [c (chan)]
          (sub pub-main :load-config c)
          
          (go-loop [config-file (-> c <! :load-config)]    
            (println "Loading RethinkDB config from " config-file)
            
            ;; load configuration file
            (let [json (load-config! config-file)]

              ;; set configuration
              (swap! app assoc :reathinkdb-config json)
              
              ;; unsub and don't recur
              (unsub pub-main :load-config c)

              (>! ch-ready true))))
        
        (defn rethinkdb-config
          ([]
             (:reathinkdb-config @app))
          ([attr]
             (-> @app :reathinkdb-config (aget attr))))

        (defn rethinkdb-conn []
          (:reathinkdb-conn @app))

        (defn rethinkdb-conn-set! [conn]
          (swap! app assoc :reathinkdb-conn conn))

        (defn rethinkdb-safe-error? [err]
          (re-find #"already exists" (str err)))

        ;; make rethinkdb connection
        (let [c (chan)]  
          (sub pub-main :rethinkdb-create-connection c)

          (go-loop [_ (<! c)]
            (println "Creating RethinkDB connection")
            
            ;; create connection for rethinkdb
            (doto r
              (.connect (rethinkdb-config)
                        (fn [err conn]
                          (if err
                            (println "Couldn't create RethinkDB connection "
                                     err))
                          (do (rethinkdb-conn-set! conn)
                              (put! ch-ready true)))))
            
            (unsub pub-main :rethinkdb-create-connection c)))

        ;; check/create rethinkdb database
        (let [c (chan)]
          (sub pub-main :rethinkdb-create-db c)
          
          (go-loop [_ (<! c)
                    db-name (rethinkdb-config "db")]
            (println "Checking/Creating main RethinkDB => " db-name)

            (try              
              (-> r
                  (.dbCreate db-name)
                  (.run (rethinkdb-conn)
                        (fn [err _]
                          (let [success! #(put! ch-ready true)]
                            (if (and err
                                     (not
                                      (rethinkdb-safe-error? err)))
                              (println "Couldn't create RethinkDB database => "
                                       db-name "\n - \n" err)
                              (success!))))))
              
              (catch js/Object ex (println ex)))

            ;; no recur
            (unsub pub-main :rethinkdb-create-db c)))
        
        ;; create rethink-db POSSIBLEDB table(s)
        (let [c (chan)]
          (sub pub-main :create-possibledb-tables c)

          (go-loop [_ (<! c)
                    table (rethinkdb-possibledb-table)
                    db (rethinkdb-config "db")]    

            (println "Checking/Creating POSSIBLEDB table(s) => " table)

            (try      
              (-> r
                  (.db db)
                  (.tableCreate table)
                  (.run (rethinkdb-conn)
                        (fn [err _]
                          (if (and err
                                   (not
                                    (rethinkdb-safe-error? err)))
                            (println "Couldn't create RethinkDB table => "
                                     table "\n - \n" err)
                            (put! ch-ready true)))))
              
              (catch js/Object ex (println ex)))

            ;; no recur
            (unsub pub-main :create-possibledb-tables c)))

        (defn rethinkdb-insert-possibledb!
          [name possibledb]
          (let [data (clj->json {:id name :data @possibledb})]
            (-> r
                (.table name)
                (.insert data)
                (.run (rethinkdb-conn)
                      (fn [err _]
                        (if err
                          (println "Couldn't create POSSIBLEDB "
                                   name "\n-\n" err)))))))

        (defn possibledb-create-db!
          ([name cb] (possibledb-create-db! name false cb))
          ([name schema cb]  
             (go-loop [name (possibledb-db-name name)
                       possibledb-db (if schema
                                 (d/create-conn schema)
                                 (d/create-conn))]
               (-> r
                   (.tableCreate name)
                   (.run (rethinkdb-conn)
                         (fn [err x]
                           (if err
                             (println "Couldn't create POSSIBLEDB "
                                      name "\n-\n" err)
                             (do
                               (rethinkdb-insert-possibledb! name possibledb-db)
                               (cb x)
                               (put! ch-ready true)))))))))

        (defn possibledb-destroy-db!
          [name cb]
          (let [name (possibledb-db-name name)
                db-name (rethinkdb-config "db")]
            (-> r
                (.db db-name)
                (.tableDrop name)
                (.run (rethinkdb-conn)
                      (fn [err msg]
                        (if-not err
                          (cb true)
                          (do
                            (println "Couldn't destroy POSSIBLEDB "
                                     name "\n - \n" err)
                            (cb false))))))))

        (defn possibledb-get-db!
          [name cb]
          (let [name (possibledb-db-name name)
                db-name (rethinkdb-config "db")]    
            (-> r
                (.db db-name)
                (.table name)
                (.filter (clj->json {:id name}))
                (.run (rethinkdb-conn)
                      (fn [err x]
                        (if err
                          (do (println "Couldn't get/filter from POSSIBLEDB "
                                       name "\n-\n" err))
                          (-> x
                              (.toArray
                               (fn [err res]
                                 (let [data (-> res
                                                js->clj
                                                first
                                                (get "data"))
                                       {:strs [schema
                                               eavt
                                               aevt
                                               avet
                                               max-eid
                                               max-tx]} data]
                                   (let [datom (fn [[e a v tx]]
                                                 (let [a (if (string? a) (keyword a) a)]
                                                   (dc/Datom. e a v tx false)))
                                         datoms (map datom eavt)
                                         add-datoms #(reduce conj % datoms)]

                                     (cb
                                      (atom
                                       (-> @(d/create-conn)
                                           (update-in [:eavt] add-datoms)
                                           (update-in [:aevt] add-datoms)
                                           (update-in [:avet] add-datoms)
                                           (assoc-in [:max-eid] max-eid)
                                           (assoc-in [:max-tx]  max-tx))
                                       :meta {:listeners (atom {})})))))))))))))

        (let [c (chan)]
          
          (defn possibledb-transact! [name data cb]
            (possibledb-get-db! name
                          (fn [conn]
                            (put! c [name conn data cb]))))

          (go-loop []
            (let [[name conn data cb] (<! c)
                  name (possibledb-db-name name)
                  db-name (rethinkdb-config "db")
                  update-document (atom nil)]

              (d/transact! conn data)

              ;; clj->js throws an error when working with
              ;; Datoms. This is a workaround and probably
              ;; shouldn't be touched
              
              (let [without-type (zipmap (keys @conn)
                                         (vals @conn))
                    document (clj->json
                              {:id name
                               :data
                               (loop [wt without-type
                                      x [:eavt :aevt :avet]]
                                 (if-not (seq x)
                                   wt
                                   (let [y (first x)]
                                     (recur
                                      (update-in wt [y] #(map vec %))
                                      (rest x))))) })]
                (reset! update-document document))

              (-> r
                  (.db db-name)
                  (.table name)
                  (.get name)
                  (.update @update-document)
                  (.run (rethinkdb-conn)
                        (fn [err x]
                          (if err
                            (println "Couldn't update POSSIBLEDB DB " name
                                     "\n-\n" err)
                            (do
                              (cb x)
                              (println "Updated POSSIBLEDB DB" name
                                       "\n-\n" x)))))))
            (recur)))

        (let [c (chan)]

          (defn possibledb-q [name q cb]
            (possibledb-get-db! name
                          (fn [conn]
                            (put! c [conn q cb]))))

          (go-loop []
            (let [[conn q cb] (<! c)]
              (cb
               (d/q q @conn)))
            (recur)))
        

        (let [c (chan)]
          (sub pub-main :incoming c)

          (go-loop []
            (let [[socket data] (-> c <! :incoming)
                  
                  ;; needed to be evaluated so must remove parens
                  safe (-> data
                           (.toString "utf8")
                           (s/replace #"\(" "")
                           (s/replace #"\)" "")
                           reader/read-string)]

              (try
                (let [[action db & args] safe
                      db (str db)
                      write! (fn [data]
                               (let [data (.toString data "utf8")]
                                 (doto socket
                                   (.write data)
                                   .end)))]
                    
                    (case (str action)
                      
                      ;; get the entire database
                      "get"
                      (possibledb-get-db! db
                                          (fn [db]
                                            (let [conn @db
                                                  
                                                  ;; remove type for reading
                                                  conn (zipmap (keys conn) (vals conn))
                                                  
                                                  ;; remove children type
                                                  vs (mapv (fn [x]
                                                             (if-not (set? x)
                                                               x
                                                               (map #(into [] %) x)))
                                                           (vals conn))
                                                  conn (zipmap (keys conn) vs)]
                                              (write! conn))))

                      "create!"
                      (let [schema (or (first args) {})]
                        (possibledb-create-db! db
                                               schema
                                               write!))

                      "destroy!"
                      (possibledb-destroy-db! db
                                              write!)
                      
                      "query"
                      (let [data (first args)]
                        (possibledb-q db
                                      data
                                      (fn [result]
                                        (write! result))))

                      "transact!"
                      (let [data (first args)]
                        (possibledb-transact! db
                                              data
                                              (fn [result]
                                                (write! result))))))

                  (catch js/Object ex
                    (println ex "input =>" safe)
                    (.end socket))))
                      
            (recur)))
        
        
        (go
          ;; load rethinkdb json config
          (>! ch-main
              {:load-config config-file})
          
          ;; park until ready, connect to rethink db
          (if (<! ch-ready)
            (>! ch-main
                {:rethinkdb-create-connection true}))
          
          ;; park until ready, create main db
          (if (<! ch-ready)
            (>! ch-main
                {:rethinkdb-create-db true}))

          ;; park until ready, create possibledb databases table
          (if (<! ch-ready)
            (>! ch-main
                {:create-possibledb-tables true})))
        
        (let [port (or port 12345)]
          
          (println "Listening on " port)
          
          (-> net
              (.createServer
               (fn [socket]
                 (-> socket
                     (.on "data"
                          (fn [data]
                            (put! ch-main {:incoming [socket data]}))))))
              
              (.listen port)))))



          ;; (when (<! ch-ready)
          ;;   (possibledb-create-db! "example")
          ;;   (<! (timeout 1000))            
          ;;   (possibledb-transact! "example"
          ;;                   [{:db/id -1 :name "user1"}])
          ;;   (<! (timeout 1000))
          ;;   (possibledb-q "example"
          ;;           '[:find ?n
          ;;             :where [?e :name ?n]])))))
