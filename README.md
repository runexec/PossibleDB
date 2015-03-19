PossibleDB
==========

![logo.png](possibledb/logo.png)

PossibleDB is a Database Server built with DataScript, RethinkDB, Clojure, ClojureScript, and NodeJS.

*WARNING: HIGHLY ALPHA*

[PossibleDB demo video](http://vimeo.com/107237345)<br />
[Active Developement Branch](https://github.com/runexec/PossibleDB/tree/dev)<br />
[Installing Latest Version](#possibledb-server-latest)<br />
[Download Releases](#releases)<br />
[Clojure Client API for all versions](#clojure-client)<br />
[Documentation](#documentation)<br />
[Change Log](CHANGES.md)<br />
[Important Links](#important-links)<br />
[License](#license)<br />

## PossibleDB Server (latest)

1)```git clone https://github.com/runexec/PossibleDB```

2)```cd PossibleDB/possibledb/```

3)```npm install rethinkdb```

4) ```rethinkdb``` (in another terminal)

5)```chmod +x possibledb.js```

6)```./possibledb.js {optional port}```


## Releases

All releases are located [here](https://github.com/runexec/PossibleDB/releases)

## Clojure Client

#### Client 1.6 for Server 0.1-6

<b>Leiningen Install</b><br />

```[possibledb-client "1.6"]```

<b>API Documentation</b>

```clojure

(:require [possibledb-client.core :as db])

(db/connect!
 "Connect to a PossibleDB server (String host Integer port)"
 [host port])

(db/get
 "Returns an entire database."
 [^:String db-name])

(db/q
 "Same q call as in DataScript"
 [^:String db-name
  query-coll])

(db/transact!
 "Same transact! call as in DataScript"
 [^:String db-name
  data-coll])

(db/create-db!
 "Create a PossibleDB db. If schema, same as DataScript."
 ([^:String db-name])
 ([^:String db-name
   ^:HashMap schema]))

(db/destroy-db!
 "Remove a DB and all of it's data"
 [^:String db-name])

(db/backup-db!
 "Writes an EDN representation of a DB to a file"
 [^:String db-name
  ^:String save-file-path])

(db/spawn-db!
 "Create a new database with all the data from original-db-name"
 [^:String original-db-name
  ^:String new-db-name])

(db/reset-db!
 "Destroy a DB and create it again"
 ([^:String db-name])
 ([^:String db-name
   ^:HashMap schema]))
  
```

#### Client 1.0 for Server 0.1


<b>Leiningen Install</b><br />

```[possibledb-client "1.0"]```

<b>API Documentation</b>

```clojure

(:require [possibledb-client.core :as db])

(db/connect! [host port])

(db/q
 "Same q call as in DataScript"
 [^:String db-name
  query-coll])

(db/transact!
 "Same transact! call as in DataScript"
 [^:String db-name
  data-coll])

(db/create-db!
 "Create a PossibleDB db"
 [^:String db-name])

```

# Documentation

PossibleDB is the bridge between DataScript and RethinkDB. Please refer to https://github.com/tonsky/datascript

<b>Running Tests</b>

<br />

```bash
cd possibledb/
npm install chai
lein do cljsbuild clean, cljsbuild once test
chmod +x test.js; ./test.js
```

Tests are located in possibledb/test/core.cljs. A mini-framework based on latte-chai is used.


# Important Links

Datomic - http://docs.datomic.com/

DataScript - https://github.com/tonsky/datascript

RethinkDB - http://rethinkdb.com/

Latte Chai - https://github.com/contentjon/chai-latte

# License 

Eclipse Public License - v 1.0
