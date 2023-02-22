(ns datomic-helper.entity
  (:require [clojure.set :as cset]
            [clojure.walk :as walk]
            [datomic.api :as d]))

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

(defn upsert-foreign!
  ([dcontext conn foreign-ks foreign-id ref-ks main-ks main-id]
   (let [found-entity (find-by-id dcontext conn foreign-ks foreign-id)]
     (when found-entity
       ((:transact dcontext) conn
                             [[:db/add [main-ks main-id]
                               ref-ks [foreign-ks foreign-id]]]))))
  ([conn foreign-ks foreign-id ref-ks main-ks main-id]
   (upsert-foreign! database-context conn foreign-ks foreign-id ref-ks main-ks main-id)))

(defn delete!
  ([dcontext conn ks id]
   (let [lookup-ref [ks id]]
     ((:transact dcontext)
      conn
      [[:db/retractEntity lookup-ref]])))
  ([conn ks id]
   (delete! database-context conn ks id)))
