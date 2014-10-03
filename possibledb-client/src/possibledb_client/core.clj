(ns possibledb-client.core
  (:refer-clojure :exclude [get]))

(def reload-conn (atom []))

(def connection (atom nil))

(defn- conn [host port]
  (java.net.Socket. host port))

(defn connect!
  "Connect to a PossibleDB server"
  [host port]
  (reset! reload-conn [host port])
  (reset! connection
          (conn host port)))

(defn reload-conn!
  "Reloads connection after remote socket is closed.
  Avoid direct calls to this function."
  []
  (apply connect! @reload-conn))

(defn possibledb-call
  "Sends valid request syntax to the current connection."
  [^:String call]
  (let [c @connection]    
    (with-open [in (clojure.java.io/reader c)                
                out (clojure.java.io/writer c)
                o *out*]               
      
      (binding [*out* out]
        (println call)
        (flush)
        (binding [*out* o
                  *read-eval* false]
          (let [response (reduce str (line-seq in))]
            (reload-conn!)
            (read-string
             response)))))))

(defn get
  "Returns an entire database."
  [^:String db-name]
  (possibledb-call
   (format "[get %s]"
           db-name)))

(defn q
  "Same q call as in DataScript"
  [^:String db-name
   query-coll]
  (possibledb-call
   (format "[query %s %s]"
           db-name
           (doall query-coll))))

(defn transact!
  "Same transact! call as in DataScript"
  [^:String db-name
   data-coll]
  (possibledb-call
   (format "[transact! %s %s]"
           db-name
           (doall data-coll))))

(defn create-db!
  "Create a PossibleDB db. If schema, same as DataScript."
  ([^:String db-name]
     (create-db! db-name {}))
  ([^:String db-name
    ^:HashMap schema]
     (possibledb-call
      (format "[create! %s %s]"
              db-name
              schema))))

(defn destroy-db!
  "Remove a DB and all of it's data"
  [^:String db-name]
  (possibledb-call
   (format "[destroy! %s]"
           db-name)))

(defn backup-db!
  "Writes an EDN representation of a DB to a file"
  [^:String db-name
   ^:String save-file-path]
  (with-open  [w (clojure.java.io/writer save-file-path
                                         :encoding "UTF-8")]
    (.write w
            (->> db-name
                 get
                 str
                 char-array))))
(defn spawn-db!
  "Create a new database with all the data from original-db-name"
  [^:String original-db-name
   ^:String new-db-name]
  (possibledb-call
   (format "[spawn! %s %s]"
           original-db-name
           new-db-name)))

(defn reset-db!
  "Destroy a DB and create it again"
  ([^:String db-name]
     (reset-db! db-name {}))
  ([^:String db-name
    ^:HashMap schema]
     (destroy-db! db-name)
     (create-db! db-name schema)))
