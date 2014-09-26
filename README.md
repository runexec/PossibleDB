![logo.png](possibledb/logo.png)

PossibleDB
==========

PossibleDB is a Datomic Database Server clone built with DataScript, RethinkDB, Clojure, ClojureScript, and NodeJS.

*warning: highly alpha*

## PossibleDB Server

1)```git clone https://github.com/runexec/PossibleDB```

2)```cd PossibleDB/possibledb/```

3)```npm install rethinkdb```

4)```chmod +x possibledb.js```

5)```./possibledb.js {optional port}```

## Clojure Client

```clojure

(:require [possibledb-client.core :as db])

(db/connect! [host port])

(db/q [^:String db-name query-coll])

(db/transact! [^:String db-name data-coll])

(db/create-db! [^:String db-name])

```

Leiningen

```[possibledb-client "1.0"]```

Gradle

```compile "possibledb-client:possibledb-client:1.0"```

Maven

```
<dependency>
<groupId>possibledb-client</groupId>
<artifactId>possibledb-client</artifactId>
<version>1.0</version>
</dependency>
```
