(ns possibledb-client.core
  (:refer-clojure :exclude [get]))

(def reload-conn (atom []))

(def connection (atom nil))

(defn- conn [host port]
  (java.net.Socket. host port))

(defn connect! [host port]
  (reset! reload-conn [host port])
  (reset! connection
          (conn host port)))

(defn reload-conn! []
  (apply connect! @reload-conn))

(defn possibledb-call [^:String call]
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

(defn get [^:String db-name]
  (possibledb-call
   (format "[get %s]"
           db-name)))

(defn q [^:String db-name query-coll]
    (possibledb-call
     (format "[query %s %s]"
             db-name
             (doall query-coll))))

(defn transact! [^:String db-name data-coll]
  (possibledb-call
   (format "[transact! %s %s]"
           db-name
           (doall data-coll))))

(defn create-db! [^:String db-name]
  (possibledb-call
     (format "[create! %s]"
             db-name)))
