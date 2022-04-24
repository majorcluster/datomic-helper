(ns datomic-helper.entity-test
  (:require [clojure.test :refer :all]
            [datomic-helper.entity :refer :all]))

(defn test-database-context
  [result]
  {:q (fn [_ & _]
        result)
   :transact (fn [_ txs]
               txs)
   :db (fn [_ & _]
         nil)})

(deftest transform-out-test
  (testing "Not having db id, result keeps the same content"
    (is (= {:product/color "rot"}
           (transform-out {:product/color "rot"})))
    (is (= {:product/color "rot"
            :product/id 1}
           (transform-out {:product/color "rot"
                          :product/id 1}))))
  (testing "Having db id it is removed"
    (is (= {:product/color "rot"}
           (transform-out {:db/id 1
                           :product/color "rot"})))
    (is (= {:product/color "rot"
            :product/id 1}
           (transform-out {:db/id 93939383
                           :product/color "rot"
                           :product/id 1})))))

(deftest find-by-id-test
  (testing "when result is a map it is returned and transformed"
    (let [db-entry {:db/id 123323
                    :stone-type :mafic-rock}
          db-context (test-database-context db-entry)]
      (is (= {:stone-type :mafic-rock}
             (find-by-id db-context {} :stone-type :mafic-rock)))))
  (testing "when result is nil it is returned"
    (let [db-context (test-database-context nil)]
      (is (= nil
             (find-by-id db-context {} :stone-type :mafic-rock)))))
  (testing "when result is empty nil is returned"
    (let [db-context (test-database-context {})]
      (is (= nil
             (find-by-id db-context {} :stone-type :mafic-rock))))))

(deftest find-all-test
  (testing "when result is nil, nil is returned"
    (let [db-context (test-database-context nil)]
      (is (= nil
             (find-all db-context {} :stone-type)))))
  (testing "when result is empty, empty is returned"
    (let [db-context (test-database-context [])]
      (is (= []
             (find-all db-context {} :stone-type)))))
  (testing "when result is a map col, it is returned"
    (let [db-entries [{:stone-type :mafic-rock}]
          db-context (test-database-context db-entries)]
      (is (= db-entries
             (find-all db-context {} :stone-type)))))
  (testing "when result is a map col with db ids, it is returned without db id"
    (let [db-entries [{:db/id 34233939
                       :stone-type :mafic-rock}]
          db-context (test-database-context db-entries)]
      (is (= [{:stone-type :mafic-rock}]
             (find-all db-context {} :stone-type))))))

(deftest update-test
  (testing "when found entity is completely different than to be saved, nothing is returned"
    (let [found-entity {:age 39}
          to-be-saved   {:name "Rosa"}
          db-context (test-database-context nil)]
      (is (= []
             (update! db-context {} :name "Rosa" found-entity to-be-saved)))))
  (testing "when no found entity is provided, nothing is returned"
    (let [found-entity {}
          to-be-saved   {:name "Rosa"}
          db-context (test-database-context nil)]
      (is (= []
             (update! db-context {} :name "Rosa" found-entity to-be-saved)))
      (is (= []
             (update! db-context nil :name "Rosa" found-entity to-be-saved)))))
  (testing "when no to-be-saved is provided, nothing is returned"
    (let [found-entity {:age 39}
          to-be-saved   {}
          db-context (test-database-context nil)]
      (is (= []
             (update! db-context {} :name "Rosa" found-entity to-be-saved)))
      (is (= []
             (update! db-context {} :name "Rosa" found-entity nil)))))
  (testing "when maps overlap, the intersection is returned as db/cas col"
    (let [db-context (test-database-context nil)]
      (is (= [[:db/cas [:name "Rosa"] :age 35 49]]
             (update! db-context {} :name "Rosa" {:age 35, :name "Rosa"} {:age 49})))
      (is (= [[:db/cas [:name "Rosa"] :age 35 49]]
             (update! db-context {} :name "Rosa" {:age 35, :name "Rosa"} {:age 49, :country :DE})))
      (is (= [[:db/cas [:name "Rosa"] :age 35 49]]
             (update! db-context {} :name "Rosa" {:age 35, :name "Rosa", :gender :woman} {:age 49, :country :DE})))
      (is (= [[:db/cas [:name "Rosa"] :age 35 49]
              [:db/cas [:name "Rosa"] :city "Zurich" "Berlin"]]
             (update! db-context {} :name "Rosa" {:age 35, :name "Rosa", :city "Zurich"} {:age 49, :city "Berlin"}))))))

(deftest upsert-test
  (testing "when entity is found, db/cas cols are returned"
    (let [db-context (test-database-context {:age 35, :name "Rosa"})]
      (is (= [[:db/cas [:name "Rosa"] :age 35 49]]
             (upsert! db-context {} [:name "Rosa"] {:age 49})))))
  (testing "when entity is not found, db/cas cols are returned"
    (let [db-context (test-database-context nil)]
      (is (= [{:age 49}]
             (upsert! db-context {} [:name "Rosa"] {:age 49}))))))