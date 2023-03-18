(ns datomic-helper.playground
  (:require [datomic.api :as d]))

#_(defonce db-uri "datomic:free://localhost:4334/clj-state-machine")
(defonce db-uri "datomic:mem:/clj-state-machine")

(defonce state-machine-specs
  '(#:db{:ident :status/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :status/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :transition/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :transition/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :transition/status-from, :cardinality :db.cardinality/one, :valueType :db.type/ref}
    #:db{:ident :transition/status-to, :cardinality :db.cardinality/one, :valueType :db.type/ref}
    #:db{:ident :workflow/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :workflow/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :workflow/transitions, :cardinality :db.cardinality/many, :valueType :db.type/ref, :isComponent true}))

(defonce another-specs
  '(#:db{:ident :movie/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :movie/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :actor/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :actor/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :movie/actors, :cardinality :db.cardinality/many, :valueType :db.type/ref, :isComponent true}
    #:db{:ident :city/id, :cardinality :db.cardinality/one, :valueType :db.type/uuid, :unique :db.unique/identity}
    #:db{:ident :city/name, :cardinality :db.cardinality/one, :valueType :db.type/string}
    #:db{:ident :actor/city, :cardinality :db.cardinality/one, :valueType :db.type/ref}))

(defn connect! []
  (d/connect db-uri))

(defn create-db! []
  (d/create-database db-uri))

(defn create-schema!
  [conn specs]
  (d/transact conn specs))

(defn add-movie-schema
  []
  (create-schema! (connect!) another-specs))

(defn start-db
  []
  (create-db!)
  (let [conn (connect!)
        specs (concat state-machine-specs another-specs)]
    (create-schema! conn specs)))

;find all
#_(d/q '[:find [(pull ?e [*]) ...]
         :in $
         :where [?e :workflow/id]] (d/db (connect!)))

;find one
#_(d/q '[:find (pull ?e [*]) .
         :in $ ?id
         :where [?e :workflow/id ?id]]
       (d/db (connect!))
       #uuid"42c32539-a437-4f73-8438-3a0ff836f2dc")

#_(d/q '[:find [(pull ?e [*]) ...]
       :in $ ?id
       :where [?e :workflow/id ?id]
              [?e :workflow/transitions ?t-e]
              [?t-e :transition/id]]
     (d/db (connect!))
     #uuid"42c32539-a437-4f73-8438-3a0ff836f2dc")

;insert
#_(d/transact (connect!)
              [{:workflow/name "wf-2", :workflow/id (UUID/randomUUID)}])

;insert entity and foreign entity, being entity many cardinal mapped at foreign
#_(d/transact (connect!)
              [{:transition/name "t-1", :transition/id (UUID/randomUUID), :db/id "temp-id"}
               {:workflow/id (UUID/randomUUID), :workflow/name "wf-3", :workflow/transitions "temp-id"}])

;insert entity and foreign entity, both works:
#_(d/transact (connect!) [{:actor/id (java.util.UUID/randomUUID), :actor/name "test", :actor/city {:city/id #uuid"745715a4-7b1e-4f13-8d46-9a52049434ce", :city/name "havana"}}])
#_(d/transact (connect!) [{:actor/id (java.util.UUID/randomUUID), :actor/name "Lenin", :actor/city [:city/id #uuid"745715a4-7b1e-4f13-8d46-9a52049434ce"]}])

;safe update
#_(d/transact (connect!)
              [[:db/cas [:workflow/id #uuid"37f04f1e-15ab-467c-9a96-fe4e96996403"] :workflow/name "wf-3" "wf-3b"]])

#_(d/transact (connect!)
              [[:db/cas [:person/group 17] :age 45 49]])
;update entity and foreign entity, being entity many cardinal mapped at foreign
#_(d/transact (connect!)
              [[:db/cas
                [:transition/id #uuid"abdac526-1630-4812-9a43-57416223e72a"]
                :transitions/_workflow
                [:workflow/id #uuid"37f04f1e-15ab-467c-9a96-fe4e96996403"]
                [:workflow/id #uuid"abb561e3-404f-4da5-b7e4-139b9f654cfa"]]])

;find all
#_(d/q '[:find [(pull ?e [*]) ...]
         :in $
         :where [?e :workflow/id]] (d/db (connect!)))
(defn find-all
  [id-ks]
  (d/q '[:find [(pull ?e [*]) ...]
         :in $ ?id-ks
         :where [?e ?id-ks]] (d/db (connect!)) id-ks))

;find one
#_(d/q '[:find (pull ?e [*]) .
         :in $ ?id
         :where [?e :workflow/id ?id]]
       (d/db (connect!))
       #uuid"42c32539-a437-4f73-8438-3a0ff836f2dc")
(defn find-one
  [attr attr-val]
  (d/q '[:find (pull ?e [*]) .
         :in $ ?attr ?attr-val
         :where [?e ?attr ?attr-val]] (d/db (connect!)) attr attr-val))

(defn find-actor-complete
  []
  (d/q '[:find [(pull ?e [* {:actor/city [*]}]) ...]
         :in $ ?attr
         :where [?e ?attr]] (d/db (connect!)) :actor/id))

(defn find-2nd-depth
  [entity-ks]
  (d/q '[:find [(pull ?e [*]) ...]
         :in $ ?attr
         :where [?e ?attr]] (d/db (connect!)) entity-ks))

(defn find-transition-complete
  []
  (d/q '[:find [(pull ?e [* {:transition/status-from [*] :transition/status-to [*]}]) ...]
         :in $ ?attr
         :where [?e ?attr]] (d/db (connect!)) :transition/id))

;insert
#_(d/transact (connect!)
              [{:city/name "havana", :city/id (java.util.UUID/randomUUID)}])
#_(d/transact (connect!)
              [{:actor/name "Angela Davis", :actor/id (java.util.UUID/randomUUID)}])

;insert entity and foreign entity, being entity many cardinal mapped at foreign
#_(d/transact (connect!)
              [{:city/name "leningrad", :city/id (java.util.UUID/randomUUID), :db/id "city-id"}
               {:actor/id (java.util.UUID/randomUUID), :actor/name "Lenin", :actor/city "city-id", :db/id "actor-id"}
               {:movie/id (java.util.UUID/randomUUID), :movie/name "Revolution", :movie/actors "actor-id"}])

;safe update
#_(d/transact (connect!)
              [[:db/cas [:city/id #uuid"6078c655-cff3-4194-9743-0358b201b8b3"] :city/name "leningrad" "Leningrad"]])

;update one-to-one foreign entity (cas does not work with refs)
#_(d/transact (connect!)
              [[:db/add
                [:actor/id #uuid"33a41ad4-0b82-4f07-b431-5a1d7320e9d7"]
                :actor/city [:city/id #uuid"bee38b5d-0bf6-45cf-b60b-ffff34f934e7"]]])

;update many-to-one foreign entity (cas does not work with refs)
#_(d/transact (connect!)
              [[:db/add
                [:movie/id #uuid"45555599-683d-47c3-bace-4362cea6157f"]
                :movie/actors [:actor/id #uuid"b23390b3-af73-4581-98cc-e3653a8c482f"]]])

;retract many-to-one foreign entity (cas does not work with refs)
#_(d/transact (connect!)
              [[:db/retract
                [:movie/id #uuid"45555599-683d-47c3-bace-4362cea6157f"]
                :movie/actors [:actor/id #uuid"b23390b3-af73-4581-98cc-e3653a8c482f"]]])

(defn insert-movies-sample
  []
  (d/transact (connect!)
              [{:city/name "leningrad", :city/id (java.util.UUID/randomUUID), :db/id "city-id"}
               {:actor/id (java.util.UUID/randomUUID), :actor/name "Lenin", :actor/city "city-id", :db/id "actor-id"}
               {:movie/id (java.util.UUID/randomUUID), :movie/name "Revolution", :movie/actors "actor-id"}]))

;missing usage, when not having a datom
(defn find-all-initial
        [workflow-id]
        (let [db (d/db (connect!))
              q '[:find [(pull ?e [* {:transition/status-to [*]}]) ...]
                  :in $ ?id-ks ?workflow-id
                  :where [?wf-e :workflow/id ?workflow-id]
                  [?wf-e :workflow/transitions ?e]
                  [(missing? $ ?e :transition/status-from)]
                  [?e ?id-ks]]]
          (->> (d/q q db :transition/id workflow-id)
               (datomic-helper.entity/transform-out))))

;3 depth joins for condition
(defn find-all-by-workflow-and-status-from
        [workflow-id status-from]
        (let [db (d/db (connect!))
              q '[:find [(pull ?e [* {:transition/status-from [*] :transition/status-to [*]}]) ...]
                  :in $ ?id-ks ?workflow-id ?status-from
                  :where [?wf-e :workflow/id ?workflow-id]
                  [?wf-e :workflow/transitions ?e]
                  [?e :transition/status-from ?s]
                  [?s :status/id ?status-from]
                  [?e ?id-ks]]]
          (->> (d/q q db :transition/id workflow-id status-from)
               (datomic-helper.entity/transform-out))))
