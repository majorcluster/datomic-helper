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
  ([dcontext conn id-ks id pull-opts]
   (let [db ((:db dcontext) conn)
         q '[:find (pull ?e pattern) .
             :in $ ?id-ks ?id pattern
             :where [?e ?id-ks ?id]]]
     (->> pull-opts
          ((:q dcontext) q db id-ks id)
          (transform-out))))
  ([dcontext conn id-ks id]
   (find-by-id dcontext conn id-ks id '[*]))
  ([conn id-ks id]
   (find-by-id database-context conn id-ks id)))

(defn find-all
  ([dcontext conn id-ks pull-opts]
    (let [db ((:db dcontext) conn)
          q '[:find [(pull ?e pattern) ...]
              :in $ ?id-ks pattern
              :where [?e ?id-ks]]]
      (->> pull-opts
           ((:q dcontext) q db id-ks)
           (transform-out))))
  ([dcontext conn id-ks]
   (find-all dcontext conn id-ks '[*]))
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

(defn insert-foreign!
  ([dcontext conn parent-ks parent-id child-ks child]
   (let [temp-id "temp-id"
         complete (merge child {:db/id temp-id})
         parent-ref {parent-ks parent-id
                      child-ks temp-id}]
     ((:transact dcontext) conn [complete parent-ref])))
  ([conn parent-ks parent-id child-ks child]
   (insert-foreign! database-context conn parent-ks parent-id child-ks child)))

(defn upsert-foreign!
  ([dcontext conn [id-ks id] parent-ks parent-id child-ks to-be-saved]
   (let [found-entity (find-by-id dcontext conn id-ks id)]
     (cond found-entity
           (update! dcontext conn id-ks id found-entity to-be-saved)
           :else (insert-foreign! dcontext conn parent-ks parent-id child-ks to-be-saved))))
  ([conn [id-ks id] parent-ks parent-id child-ks to-be-saved]
   (upsert-foreign! database-context conn [id-ks id] parent-ks parent-id child-ks to-be-saved)))