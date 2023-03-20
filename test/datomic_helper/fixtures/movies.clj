(ns datomic-helper.fixtures.movies
  (:require [clojure.test :refer :all]
            [datomic.api :as d])
  (:import [java.util UUID]))

(defonce specs
  '(#:db{:ident :movie/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :movie/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :actor/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :actor/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :movie/actors, :cardinality :db.cardinality/many, :valueType :db.type/ref, :isComponent true}
    #:db{:ident :city/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :city/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :actor/city, :cardinality :db.cardinality/one, :valueType :db.type/ref}
    #:db{:ident :empty-datom/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}))

(def ids
  {:city-1 (UUID/randomUUID)
   :city-2 (UUID/randomUUID)
   :movie-1 (UUID/randomUUID)
   :movie-2 (UUID/randomUUID)
   :actor-1 (UUID/randomUUID)
   :actor-2 (UUID/randomUUID)
   :actor-3 (UUID/randomUUID)})

(def data
  {:city-1 {:city/name "Leningrad", :city/id (:city-1 ids)}
   :city-2 {:city/name "Havana", :city/id (:city-2 ids)}
   :movie-1 {:movie/name "1917" :movie/id (:movie-1 ids)}
   :movie-2 {:movie/name "Sitio de la isla" :movie/id (:movie-2 ids)}
   :actor-1 {:actor/name "Lenin" :actor/city [:city/id (:city-1 ids)] :actor/id (:actor-1 ids)}
   :actor-2 {:actor/name "Celia Sanchez" :actor/city [:city/id (:city-2 ids)] :actor/id (:actor-2 ids)}
   :actor-3 {:actor/name "El Fidel" :actor/city [:city/id (:city-2 ids)] :actor/id (:actor-3 ids)}})

(defn insert-samples
  [conn]
  (d/transact conn
              [(:city-1 data) (:city-2 data)])
  (d/transact conn
              [(:movie-1 data) (:movie-2 data)])
  (d/transact conn
              [(:actor-1 data) (:actor-2 data) (:actor-3 data)])
  (d/transact conn
              [[:db/add [:movie/id (:movie-1 ids)]
                :movie/actors [:actor/id (:actor-1 ids)]]
               [:db/add [:movie/id (:movie-2 ids)]
                :movie/actors [:actor/id (:actor-2 ids)]]
               [:db/add [:movie/id (:movie-2 ids)]
                :movie/actors [:actor/id (:actor-3 ids)]]]))
