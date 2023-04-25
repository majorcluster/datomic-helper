(ns datomic-helper.entity
  (:require [clojure.set :as cset]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [datomic.api :as d]))

(defn- transform-nested-out
  [result]
  (if (map? result)
    (dissoc result :db/id)
    result))

(defn transform-out
  "transforms d/q query results by removing db/id from root and nested maps.
  ex:
  - `(transform-out {:db/id 12 :name \"Rosa\"}) => {:name \"Rosa\"}`"
  [result-seq]
  (let [transformed (walk/prewalk transform-nested-out result-seq)]
    (if (and (map? transformed)
             (empty? transformed))
      nil
      transformed)))

(defn find-by-id
  "finds single result by id, pull-opts default is '[*].
  ex:
  - `(find-by-id conn :product-id 1917) => {:product-id 1917}`
  - `(find-by-id conn :product-id 1917 '[* {:product/category [*]]}) => {:product-id 1917, :product/category {:category/id 12}}`"
  ([conn id-ks id pull-opts]
   (let [db (d/db conn)
         q '[:find (pull ?e pattern) .
             :in $ ?id-ks ?id pattern
             :where [?e ?id-ks ?id]]]
     (->> pull-opts
          (d/q q db id-ks id)
          transform-out)))
  ([conn id-ks id]
   (find-by-id conn id-ks id '[*])))

(defn v=
  [v]
  {:fn =
   :v-first? true
   :args (list v)})

(defn v-not=
  [v]
  {:fn not=
   :v-first? true
   :args (list v)})

(defn v>
  [v]
  {:fn >
   :v-first? true
   :args (list v)})

(defn v>=
  [v]
  {:fn >=
   :v-first? true
   :args (list v)})

(defn v<
  [v]
  {:fn <
   :v-first? true
   :args (list v)})

(defn v<=
  [v]
  {:fn <=
   :v-first? true
   :args (list v)})

(defn v-starts-with
  [v]
  {:fn str/starts-with?
   :v-first? true
   :args (list v)})

(defn v-ends-with
  [v]
  {:fn str/ends-with?
   :v-first? true
   :args (list v)})

(defn v-includes
  [v]
  {:fn str/includes?
   :v-first? true
   :args (list v)})

(defn v-matches
  [v]
  {:fn re-matches
   :v-first? false
   :args [v]})

(defn v-custom->
  [custom-fn & args]
  {:fn custom-fn
   :v-first? true
   :args (vec args)})

(defn v-custom->>
  [custom-fn & args]
  {:fn custom-fn
   :v-first? false
   :args (list args)})

(defn- acc-param-fn-parts
  [acc k v]
  (-> acc
      (assoc :where (concat (:where acc) (if (:v-first? v)
                                           [[(symbol "?e") k (symbol (str "?_fn_" (:fn-where-pairs-count acc)))]
                                            [(apply list (flatten
                                                          [(:fn v)
                                                           (symbol (str "?_fn_" (:fn-where-pairs-count acc)))
                                                           (-> v :args)]))]]
                                           [[(symbol "?e") k (symbol (str "?_fn_" (:fn-where-pairs-count acc)))]
                                            [(apply list (flatten
                                                          [(:fn v)
                                                           (-> v :args)
                                                           (symbol (str "?_fn_" (:fn-where-pairs-count acc)))]))]]))
             :fn-where-pairs-count (inc (:fn-where-pairs-count acc)))))

(defn- acc-param-parts
  [kvs-map]
  (reduce (fn [acc [k v]]
            (if (and (map? v)
                     (contains? v :fn)
                     (contains? v :args))
              (acc-param-fn-parts acc k v)
              (-> acc
                  (assoc :in (conj (:in acc) (symbol (str "?_" (count (:in acc)))))
                         :where (conj (:where acc) [(symbol "?e") k (symbol (str "?_" (count (:in acc))))])
                         :values (conj (:values acc) v)))))
          {:in [] :where [] :values [] :fn-where-pairs-count 0}
          kvs-map))

(defn find-by-params
  "finds results by params, pull-opts default is '[*]. kvs-map uses `=` as default strategy,
  for different matchers use `v*` functions as stated in examples bellow.
  ex:
  - `(find-by-params conn {:product-id 1917}) => [{:product-id 1917}]`
  - `(find-by-params conn {:product-id (v= 1917)}) => [{:product-id 1917}] ;equals (=)`
  - `(find-by-params conn {:product-id (v> 1916)}) => [{:product-id 1917}] ;greater than (>)`
  - `(find-by-params conn {:product-id (v>= 1917)}) => [{:product-id 1917}] ;greater than or equals (>=)`
  - `(find-by-params conn {:product-id (v< 1918)}) => [{:product-id 1917}] ;less than (<)`
  - `(find-by-params conn {:product-id (v<= 1917)}) => [{:product-id 1917}] ;less than or equals (<=)`
  - `(find-by-params conn {:product-name (v-starts-with \"lil\")}) => [{:product-id 17 :product-name \"lilek\"}] ;clojure string starts with`
  - `(find-by-params conn {:product-name (v-ends-with \"ek\")}) => [{:product-id 17 :product-name \"lilek\"}] ;clojure string ends with`
  - `(find-by-params conn {:product-name (v-includes \"le\")}) => [{:product-id 17 :product-name \"lilek\"}] ;clojure string includes`
  - `(find-by-params conn {:product-name (v-matches #\"[lilek]{1,5}\")}) => [{:product-id 17 :product-name \"lilek\"}] ;clojure regex re-matches`
  - `(find-by-params conn {:product-id (v-custom-> some-fn some-arg)}) => [{:product-id 1917}] ;custom fn with argument being passed as first argument`
  - `(find-by-params conn {:product-id (v-custom->> some-fn some-arg)}) => [{:product-id 1917}] ;custom fn with argument being passed as last argument`
  - `(find-by-params conn {:product-id 1917} '[* {:product/category [*]]}) => {:product-id 1917, :product/category {:category/id 12}}`"
  ([conn kvs-map pull-opts]
   (let [db (d/db conn)
         acc-parts (acc-param-parts kvs-map)
         q '[:find [(pull ?e pattern) ...]
             :in $ pattern]
         q (concat q (:in acc-parts))
         q (concat q [:where] (:where acc-parts))]
     (->> acc-parts
          :values
          (apply d/q q db pull-opts)
          transform-out)))
  ([conn kvs-map]
   (find-by-params conn kvs-map '[*])))

(defn find-all
  "finds all entries having a key, pull-opts default is '[*].
  ex:
  - `(find-all conn :product-id) => [{:product-id 24},{:product-id 1917}]`
  - `(find-all conn :product-id '[* {:product/category [*]]) => [{:product-id 24, :product/category {:category/id 15}},{:product-id 1917, :product/category {:category/id 12}}]`"
  ([conn id-ks pull-opts]
   (let [db (d/db conn)
         q '[:find [(pull ?e pattern) ...]
             :in $ ?id-ks pattern
             :where [?e ?id-ks]]]
     (->> pull-opts
          (d/q q db id-ks)
          transform-out)))
  ([conn id-ks]
   (find-all conn id-ks '[*])))

(defn update!
  "updates entity by matching id-key and its value, intersecting found-entity with to-be-saved, therefore using :db/cas strategy for matching datoms and :db/add for new ones.
  ex:
  - `(update! conn :product-id 1917 {:age 0} {:age 1917})`"
  [conn id-ks id found-entity to-be-saved]
  (let [attr-old (set (keys found-entity))
        attr-partial (set (keys to-be-saved))
        intersect (disj (cset/intersection attr-old attr-partial) id-ks)
        to-insert (disj (cset/difference attr-partial attr-old) id-ks)
        txs (map (fn [attr]
                   [:db/cas
                    [id-ks id]
                    attr
                    (get found-entity attr)
                    (get to-be-saved attr)])
                 intersect)
        txs (concat txs (map (fn [attr]
                               [:db/add
                                [id-ks id]
                                attr (get to-be-saved attr)])
                             to-insert))]
    (d/transact conn txs)))

(defn- reduce-parts
  [db id-ks ks-parts]
  (reduce (fn [acc-parts ks-part]
            (assoc acc-parts
              :unique-parts/in (conj (:unique-parts/in acc-parts) (:unique-parts/alias ks-part))
              :unique-parts/where (conj (:unique-parts/where acc-parts) (conj '[?e] (:unique-parts/key ks-part) (:unique-parts/alias ks-part)))
              :unique-parts/params (conj (:unique-parts/params acc-parts) (:unique-parts/value ks-part))))
          {:unique-parts/find '[:find ?e .]
           :unique-parts/in '[:in $ ?id-ks]
           :unique-parts/where '[:where [?e ?id-ks]]
           :unique-parts/params [db id-ks]}
          ks-parts))

(defn check-unique!
  "Check unique plain entity atoms, not checking nested.
  ex:
  - `(check-unique! (connect!) :city/id :city/id #uuid\"586c5e02-599e-4210-993a-f74bbdfc0e16\" :city/name \"Havana\")`"
  [conn id-ks & unique-datom-ks]
  (if (empty? unique-datom-ks)
    true
    (let [db (d/db conn)
          ks (partition 2 unique-datom-ks)
          ks-parts (map-indexed (fn [idx [k v]]
                                  {:unique-parts/alias (symbol (str "?_" idx))
                                   :unique-parts/idx idx
                                   :unique-parts/key k
                                   :unique-parts/value v}) ks)
          parts (reduce-parts db id-ks ks-parts)]
      (not (apply d/q (concat (:unique-parts/find parts)
                              (:unique-parts/in parts)
                              (:unique-parts/where parts)) (:unique-parts/params parts))))))

(defn check-unique-custom!
  "Check unique plain entity atoms, with custom where datoms.
  ex:
  - `(check-unique-custom! conn :actor/id '[[?e :actor/name ?_0] [?e :actor/city ?c] [?c :city/name ?_1]] \"Lenin\" \"Leningrad\")"
  [conn id-ks custom-datom-ks & values]
  (if (or (empty? custom-datom-ks)
          (empty? values))
    true
    (let [db (d/db conn)
          in-parts (map-indexed (fn [idx _] (symbol (str "?_" idx))) values)
          q '[:find ?e .
              :in $ ?id-ks]
          q (concat q in-parts)
          q (concat q '[:where [?e ?id-ks]])
          q (concat q custom-datom-ks)]
      (not (apply d/q q db id-ks values)))))

(defn insert!
  "inserts entity by using a simple datomic transact. Check unique possible as an optional param.
  ex:
  - `(insert! conn {:product-id 8990})`
  - `(insert! conn {:product-id 8990} my-unique-check)`"
  ([conn to-be-saved check-unique]
   (when (check-unique)
     (d/transact conn [to-be-saved])))
  ([conn to-be-saved]
   (d/transact conn [to-be-saved])))

(defn upsert!
  "upserts entity, finding it by specified id or ks and executing either **insert!** or **update!**. Check unique possible as an optional param.
  ex:
  - `(upsert! conn [:product-id 1917] {:done true})`
  - `(upsert! conn [:product-id 1917] {:done true} my-unique-check)`"
  ([conn [id-ks id] to-be-saved check-unique]
   (let [found-entity (find-by-id conn id-ks id)]
     (cond found-entity
           (update! conn id-ks id found-entity to-be-saved)
           :else (when (check-unique)
                   (insert! conn to-be-saved)))))
  ([conn [id-ks id] to-be-saved]
   (upsert! conn [id-ks id] to-be-saved (fn [] true))))

(defn upsert-foreign!
  "upserts foreign entity connected with an entity.
  ex:
  - `(upsert-foreign! conn :group/id 17 :person/group :person/id 4)`"
  [conn foreign-ks foreign-id ref-ks main-ks main-id]
  (let [found-entity (find-by-id conn foreign-ks foreign-id)]
    (when found-entity
      (d/transact conn
                  [[:db/add [main-ks main-id]
                    ref-ks [foreign-ks foreign-id]]]))))

(defn delete!
  "deletes entity by matching id-ks and its value.
  ex:
  - `(delete! conn :product-id 1917)`"
  [conn ks id]
  (let [lookup-ref [ks id]]
    (d/transact
     conn
     [[:db/retractEntity lookup-ref]])))
