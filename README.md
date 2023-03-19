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

## Migration
[Migration guide](https://github.com/mtsbarbosa/datomic-helper/tree/main/doc/MIGRATION.md)

## Documentation
### datomic-helper/entity
| Functions                                                           | Description                                                                                                                                                                                                                                                                                                           |
|---------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| transform-out [result]                                              | transforms d/q query results by removing db/id from root and nested maps <br> `(transform-out {:db/id 12 :name "Rosa"}) => {:name "Rosa"}`                                                                                                                                                                            |
| find-by-id [conn id-ks id & pull-opts]                              | finds single result by id, pull-opts default is [*] <br> `(find-by-id conn :product-id 1917) => {:product-id 1917}` <br> `(find-by-id conn :product-id 1917 [* {:product/category [*]]}) => {:product-id 1917, :product/category {:category/id 12}}`                                                                  |
| find-all [conn id-ks & pull-opts]                                   | finds all entries having a key, pull-opts default is [*] <br> `(find-all conn :product-id) => [{:product-id 24},{:product-id 1917}]` <br> `(find-all conn :product-id [* {:product/category [*]]) => [{:product-id 24, :product/category {:category/id 15}},{:product-id 1917, :product/category {:category/id 12}}]` |
| update! [conn id-ks id found-entity to-be-saved]                    | updates entity by matching id-key and its value, intersecting found-entity with to-be-saved, therefore using :db/cas strategy, not updating if the value before is not the same anymore in db <br> `(update! conn :product-id 1917 {:age 0} {:age 1917})`                                                             |
| insert! [conn to-be-saved]                                          | inserts entity by using a simple datomic transact <br> `(insert! conn {:product-id 8990})`                                                                                                                                                                                                                            |
| upsert! [conn [id-ks id] to-be-saved]                               | upserts entity, finding it by specified id or ks and executing either **insert!** or **update!** <br> `(upsert! conn [:product-id 1917] {:done true})`                                                                                                                                                                |
| upsert! [conn [id-ks id] to-be-saved unique-check]                  | upserts entity, finding it by specified id or ks and executing either **insert!** or **update!** checking uniqueness by passed fn <br> `(upsert! conn [:product-id 1917] {:done true} my-fn)`                                                                                                                         |
| upsert-foreign! [conn foreign-ks foreign-id ref-ks main-ks main-id] | upserts foreign entity connected with an entity, if foreign entity is not found, returns nil, otherwise uses :db/add to insert or update the foreign ref <br> `(upsert-foreign! conn :group/id 17 :person/group :person/id 4)`                                                                                        |
| delete! [conn id-ks id]                                             | deletes entity by matching id-ks and its value <br> `(delete! conn :product-id 1917)`                                                                                                                                                                                                                                 |

### datomic-helper/schema-transform

|Common parameters| Description |
| --------------- | ----------- |
|configs|having all keys as optional: <br> {:indexed [:ks...] :components [:ks...] :historyless [:ks...]} <br> a key sent in the list of the configs will have <br> in :indexed = db/indexed true <br> in :components = db/isComponent true <br> in :historyless = db/noHistory true"|

| Functions     | Description |
| ------------- | ----------- |
| schema-to-datomic [definition configs] | receives a prismatic/schema entity definition and converts it to datomic schema definition.<br>definition: see prismatic/schema documentation<br>configs: see configs at **Common parameters** |
| schemas-to-datomic [definitions configs] | receives a vector of prismatic/schema entities definitions and converts them to datomic schema definitions.<br>definitions: see prismatic/schema documentation<br>configs: see configs at **Common parameters** |
