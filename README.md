![logo.png](possibledb/logo.png)

PossibleDB
==========

PossibleDB is a Datomic Database Server clone built with DataScript, RethinkDB, Clojure, ClojureScript, and NodeJS.

*WARNING: HIGHLY ALPHA*

#### You can see PossibleDB in action
@ http://vimeo.com/107237345

#### Active developement branch
@ https://github.com/runexec/PossibleDB/tree/dev

#### Changes
@ https://github.com/runexec/PossibleDB/blob/dev/CHANGES.md

## PossibleDB Server (latest)

1)```git clone https://github.com/runexec/PossibleDB```

2)```cd PossibleDB/possibledb/```

3)```npm install rethinkdb```

4)```chmod +x possibledb.js```

5)```./possibledb.js {optional port}```


## Releases

https://github.com/runexec/PossibleDB/releases

## Clojure Client

#### Client 1.5 for Server 0.1-4

```clojure

(:require [possibledb-client.core :as db])

(db/connect! [host port])

(db/get
  [^:String db-name])

(db/q
  [^:String db-name])

(db/transact!
  [^:String db-name])

(db/create-db!
  ([^:String db-name])
  ([^:String db-name
    ^:HashMap schema]))

(db/destroy-db!
  [^:String db-name])

(db/backup-db!
 [^:String db-name
  ^:String save-file-path])

(db/spawn-db!
  [^:String original-db-name
   ^:String new-db-name])
  
```

#### client 1.0 for Server 0.1

```clojure

(:require [possibledb-client.core :as db])

(db/connect! [host port])

(db/q
 [^:String db-name
  query-coll])

(db/transact!
 [^:String db-name
  data-coll])

(db/create-db!
 [^:String db-name])

```

#### Leiningen

```[possibledb-client "1.0"]```

#### Gradle

```compile "possibledb-client:possibledb-client:1.0"```

#### Maven

```
<dependency>
<groupId>possibledb-client</groupId>
<artifactId>possibledb-client</artifactId>
<version>1.0</version>
</dependency>
```

# Documentation

PossibleDB is the bridge between DataScript and RethinkDB. Please refer to https://github.com/tonsky/datascript


# Important Links

Datomic - http://docs.datomic.com/

DataScript - https://github.com/tonsky/datascript

RethinkDB - http://rethinkdb.com/

# License 

Eclipse Public License - v 1.0
