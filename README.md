# datomic-helper

A Clojure library with handful tools for helping the usage of datomic-free

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.majorcluster/datomic-helper.svg)](https://clojars.org/org.clojars.majorcluster/datomic-helper)

## Usage

* Add the dependency:
```clojure
[org.clojars.majorcluster/datomic-helper "LAST RELEASE NUMBER"]
```

## Publish
### Requirements
* Leiningen (of course 😄)
* GPG (mac => brew install gpg)
* Clojars account
* Enter clojars/tokens page in your account -> generate one and use for password
```shell
export GPG_TTY=$(tty) && lein deploy clojars
```

## Documentation
### datomic-helper/entity
| Functions     | Description |
| ------------- | ----------- |
| transform-out [result] |  transforms d/q query results by removing db/id from root and nested maps <br> `(transform-out {:db/id 12 :name "Rosa"}) => {:name "Rosa"}`|
| find-by-id [conn id-ks id] | finds single result by id <br> `(find-by-id conn :product-id 1917) => {:product-id 1917}`|
| find-all [conn id-ks] | finds all entries having a key <br> `(find-all conn :product-id) => [{:product-id 24},{:product-id 1917}]`|
| update! [conn id-ks id found-entity to-be-saved] | updates entity by matching id-key and its value, intersecting found-entity with to-be-saved, therefore using :db/cas strategy, not updating if the value before is not the same anymore in db <br> `(update! conn :product-id 1917 {:age 0} {:age 1917})`|
| insert! [conn to-be-saved] | inserts entity by using a simple datomic transact <br> `(insert! conn {:product-id 8990})` |
| upsert! [conn [id-ks id] to-be-saved] | upserts entity, finding it by specified id or ks and executing either **insert!** or **update!** <br> `(upsert! conn [:product-id 1917] {:done true})` |

### datomic-helper/schema-transform

|Common parameters| Description |
| --------------- | ----------- |
|configs|having all keys as optional: <br> {:indexed [:ks...] :components [:ks...] :historyless [:ks...]} <br> a key sent in the list of the configs will have <br> in :indexed = db/indexed true <br> in :components = db/isComponent true <br> in :historyless = db/noHistory true"|

| Functions     | Description |
| ------------- | ----------- |
| schema-to-datomic [definition configs] | receives a prismatic/schema entity definition and converts it to datomic schema definition.<br>definition: see prismatic/schema documentation<br>configs: see configs at **Common parameters** |
| schemas-to-datomic [definitions configs] | receives a vector of prismatic/schema entities definitions and converts them to datomic schema definitions.<br>definitions: see prismatic/schema documentation<br>configs: see configs at **Common parameters** |
