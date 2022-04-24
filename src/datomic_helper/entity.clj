(ns datomic-helper.entity
  (:require [clojure.walk :as walk]
            [datomic.api :as d]
            [clojure.set :as cset])
  (:use clojure.pprint))

(def database-context
  {:q d/q
   :transact d/transact
   :db d/db})

(defn- transform-nested-out
  [result]
  (if (map? result)
    (dissoc result :db/id)
    result))

(defn transform-out
  [result-seq]
  (let [transformed (walk/prewalk transform-nested-out result-seq)]
    (if (and (map? transformed)
             (empty? transformed))
      nil
      transformed)))

(defn find-by-id
  ([dcontext conn id-ks id]
   (let [db ((:db dcontext) conn)
         q '[:find (pull ?e [*]) .
             :in $ ?id-ks ?id
             :where [?e ?id-ks ?id]]]
     (->> id
          ((:q dcontext) q db id-ks)
          (transform-out))))
  ([conn id-ks id]
   (find-by-id database-context conn id-ks id)))

(defn find-all
  ([dcontext conn id-ks]
    (let [db ((:db dcontext) conn)
          q '[:find [(pull ?e [*]) ...]
              :in $ ?id-ks
              :where [?e ?id-ks]]]
      (->> id-ks
           ((:q dcontext) q db)
           (transform-out))))
  ([conn id-ks]
    (find-all database-context conn id-ks)))

(defn update!
  ([dcontext
     conn
     id-ks
     id
     found-entity
     to-be-saved]
    (let [attr-old (set (keys found-entity))
          attr-partial (set (keys to-be-saved))
          intersect (disj (cset/intersection attr-old attr-partial) id-ks)
          txs (map (fn [attr]
                     [:db/cas
                      [id-ks id]
                      attr
                      (get found-entity attr)
                      (get to-be-saved attr)])
                   intersect)]
      ((:transact dcontext) conn txs)))
  ([conn
    id-ks
    id
    found-entity
    to-be-saved]
    (update! database-context conn id-ks id found-entity to-be-saved)))

(defn insert!
  ([dcontext
     conn
     to-be-saved]
    ((:transact dcontext) conn [to-be-saved]))
  ([conn to-be-saved]
   (insert! database-context conn to-be-saved)))

(defn upsert!
  ([dcontext conn [id-ks id] to-be-saved]
    (let [found-entity (find-by-id dcontext conn id-ks id)]
      (cond found-entity
            (update! dcontext conn id-ks id found-entity to-be-saved)
            :else (insert! dcontext conn to-be-saved))))
  ([conn [id-ks id] to-be-saved]
    (upsert! database-context conn [id-ks id] to-be-saved)))
