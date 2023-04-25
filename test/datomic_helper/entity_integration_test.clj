(ns datomic-helper.entity-integration-test
  (:require [clojure.test :refer :all]
            [core-test :refer [connect! test-fixture]]
            [datomic-helper.entity :as d-h.entity]
            [datomic-helper.fixtures.movies :as fixtures.movies])
  (:import [java.util UUID]))

(use-fixtures :each test-fixture)

(deftest find-by-id-test
  (testing "when found"
    (is (= (:city-1 fixtures.movies/data)
           (d-h.entity/find-by-id (connect!) :city/id (:city-1 fixtures.movies/ids))))
    (is (= (assoc (:movie-1 fixtures.movies/data)
             :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                              :actor/city {})])
           (d-h.entity/find-by-id (connect!) :movie/id (:movie-1 fixtures.movies/ids)))))
  (testing "when pull opts is sent"
    (is (= (assoc (:movie-1 fixtures.movies/data)
             :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                              :actor/city (:city-1 fixtures.movies/data))])
           (d-h.entity/find-by-id (connect!)
                                  :movie/id
                                  (:movie-1 fixtures.movies/ids)
                                  '[* {:movie/actors [* {:actor/city [*]}]}])))))

(deftest find-all-test
  (testing "when found"
    (is (= [(:city-1 fixtures.movies/data) (:city-2 fixtures.movies/data)]
           (d-h.entity/find-all (connect!) :city/id))))
  (testing "when not found"
    (is (empty? (d-h.entity/find-all (connect!) :empty-datom/id))))
  (testing "when pull opts is sent"
    (is (= [(assoc (:movie-1 fixtures.movies/data)
              :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                               :actor/city (:city-1 fixtures.movies/data))])
            (assoc (:movie-2 fixtures.movies/data)
              :movie/actors [(assoc (:actor-2 fixtures.movies/data)
                               :actor/city (:city-2 fixtures.movies/data))
                             (assoc (:actor-3 fixtures.movies/data)
                               :actor/city (:city-2 fixtures.movies/data))])]
           (d-h.entity/find-all (connect!)
                                :movie/id
                                '[* {:movie/actors [* {:actor/city [*]}]}])))
    (is (empty? (d-h.entity/find-all (connect!)
                                     :empty-datom/id
                                     '[* {:movie/actors [* {:actor/city [*]}]}])))))

(defn my-pred-> [received-q f-const plus-const]
  (> (+ received-q plus-const) f-const))

(defn my-pred->> [f-const plus-const received-q]
  (> (+ received-q plus-const) f-const))

(deftest find-by-params-test
  (testing "when found by simple match"
    (is (= [(:city-1 fixtures.movies/data)]
           (d-h.entity/find-by-params (connect!) {:city/name "Leningrad"}))))
  (testing "when found by string matchers"
    (are [matcher matching] (= [(:city-1 fixtures.movies/data)]
                               (d-h.entity/find-by-params (connect!) {:city/name (matcher matching)}))
      d-h.entity/v= "Leningrad"
      d-h.entity/v-not= "Havana"
      d-h.entity/v-starts-with "Len"
      d-h.entity/v-ends-with "grad"
      d-h.entity/v-includes "ing"
      d-h.entity/v-matches #"[a-zA-Z]{9}"))
  (testing "when multiple match"
    (is (= [(:city-1 fixtures.movies/data) (:city-2 fixtures.movies/data)]
           (d-h.entity/find-by-params (connect!) {:city/name (d-h.entity/v-matches #"[a-zA-Z]+")}))))
  (testing "when found by number matchers"
    (are [matcher matching] (= [(:movie-2 fixtures.movies/data)]
                               (->> (d-h.entity/find-by-params (connect!) {:movie/year (matcher matching)})
                                    (map #(select-keys % [:movie/id :movie/name :movie/year]))))
      d-h.entity/v= 1977
      d-h.entity/v-not= 1930
      d-h.entity/v> 1930
      d-h.entity/v>= 1931)
    (are [matcher matching] (= [(:movie-1 fixtures.movies/data) (:movie-2 fixtures.movies/data)]
                               (->> (d-h.entity/find-by-params (connect!) {:movie/year (matcher matching)})
                                    (map #(select-keys % [:movie/id :movie/name :movie/year]))))
      d-h.entity/v< 1978
      d-h.entity/v<= 1977))
  (testing "when found by custom matchers"
    (is (= [(:movie-2 fixtures.movies/data)]
           (->> (d-h.entity/find-by-params (connect!) {:movie/year (d-h.entity/v-custom-> my-pred-> 1968 10)})
                (map #(select-keys % [:movie/id :movie/name :movie/year])))))
    (is (= [(:movie-2 fixtures.movies/data)]
           (->> (d-h.entity/find-by-params (connect!) {:movie/year (d-h.entity/v-custom->> my-pred->> 1968 10)})
                (map #(select-keys % [:movie/id :movie/name :movie/year]))))))
  (testing "when not found"
    (is (empty? (d-h.entity/find-by-params (connect!) {:movie/name "none"}))))
  (testing "when pull opts is sent"
    (is (= [(assoc (:movie-1 fixtures.movies/data)
              :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                               :actor/city (:city-1 fixtures.movies/data))])
            (assoc (:movie-2 fixtures.movies/data)
              :movie/actors [(assoc (:actor-2 fixtures.movies/data)
                               :actor/city (:city-2 fixtures.movies/data))
                             (assoc (:actor-3 fixtures.movies/data)
                               :actor/city (:city-2 fixtures.movies/data))])]
           (d-h.entity/find-by-params (connect!)
                                      {:movie/year (d-h.entity/v> 1900)}
                                      '[* {:movie/actors [* {:actor/city [*]}]}])))
    (is (empty? (d-h.entity/find-by-params (connect!)
                                           {:movie/name "none"}
                                           '[* {:movie/actors [* {:actor/city [*]}]}])))))

(deftest insert-test
  (let [new-city {:city/id (UUID/randomUUID)}
        new-city-2 {:city/id (UUID/randomUUID)}]
    (testing "when inserted"
      (d-h.entity/insert! (connect!) new-city)
      (is (= new-city
             (d-h.entity/find-by-id (connect!) :city/id (:city/id new-city)))))
    (testing "when unique is violated"
      (d-h.entity/insert! (connect!) new-city-2 (fn [] false))
      (is (nil? (d-h.entity/find-by-id (connect!) :city/id (:city/id new-city-2)))))
    (testing "when unique is not violated"
      (d-h.entity/insert! (connect!) new-city-2 (fn [] true))
      (is (= new-city-2
             (d-h.entity/find-by-id (connect!) :city/id (:city/id new-city-2)))))))

(deftest update-test
  (let [incomplete-city {:city/id (UUID/randomUUID)}
        u-partial (partial d-h.entity/update! (connect!)
                           :city/id (:city/id incomplete-city)
                           (d-h.entity/find-by-id (connect!) :city/id (:city/id incomplete-city)))]
    (d-h.entity/insert! (connect!) incomplete-city)
    (testing "when updated"
      (u-partial {:city/name "Brno"})
      (is (= (assoc incomplete-city
               :city/name "Brno")
             (d-h.entity/find-by-id (connect!) :city/id (:city/id incomplete-city))))
      (u-partial {:city/name "Znojmo"})
      (is (= (assoc incomplete-city
               :city/name "Znojmo")
             (d-h.entity/find-by-id (connect!) :city/id (:city/id incomplete-city)))))
    (testing "when data outdated"
      (d-h.entity/update! (connect!)
                          :city/id (:city/id incomplete-city)
                          (assoc (d-h.entity/find-by-id (connect!) :city/id (:city/id incomplete-city))
                            :city/name "Brno")
                          {:city/name "Brno"})
      (is (= (assoc incomplete-city
               :city/name "Znojmo")
             (d-h.entity/find-by-id (connect!) :city/id (:city/id incomplete-city)))))))

(deftest upsert-test
  (let [new-actor {:actor/id (UUID/randomUUID)}
        new-actor-2 {:actor/id (UUID/randomUUID)}]
    (testing "when insert is performed"
      (d-h.entity/upsert! (connect!) [:actor/id (:actor/id new-actor)] new-actor)
      (is (= new-actor
             (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor)))))
    (testing "when update with new datom is performed"
      (d-h.entity/upsert! (connect!) [:actor/id (:actor/id new-actor)] (assoc new-actor
                                                                         :actor/name "Olga Benario"))
      (is (= (assoc new-actor
               :actor/name "Olga Benario")
             (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor)))))
    (testing "when update with changed datom is performed"
      (d-h.entity/upsert! (connect!) [:actor/id (:actor/id new-actor)] (assoc new-actor
                                                                         :actor/name "Ana Montenegro"))
      (is (= (assoc new-actor
               :actor/name "Ana Montenegro")
             (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor)))))
    (testing "when insert and non unique"
      (d-h.entity/upsert! (connect!)
                          [:actor/id (:actor/id new-actor-2)]
                          (assoc new-actor-2
                            :actor/name "Ana Montenegro")
                          (fn [] false))
      (is (nil? (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor-2)))))))

(deftest upsert-foreign-test
  (let [new-movie {:movie/id (UUID/randomUUID)}
        new-actor {:actor/id (UUID/randomUUID)}]
    (testing "when one-to-many"
      (d-h.entity/insert! (connect!) new-movie)
      (d-h.entity/upsert-foreign! (connect!)
                                  :actor/id (-> fixtures.movies/data
                                                :actor-1
                                                :actor/id)
                                  :movie/actors
                                  :movie/id (:movie/id new-movie))
      (is (= (assoc new-movie
               :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                                :actor/city {})])
             (d-h.entity/find-by-id (connect!) :movie/id (:movie/id new-movie))))
      (d-h.entity/upsert-foreign! (connect!)
                                  :actor/id (-> fixtures.movies/data
                                                :actor-2
                                                :actor/id)
                                  :movie/actors
                                  :movie/id (:movie/id new-movie))
      (is (= (assoc new-movie
               :movie/actors [(assoc (:actor-1 fixtures.movies/data)
                                :actor/city {})
                              (assoc (:actor-2 fixtures.movies/data)
                                :actor/city {})])
             (d-h.entity/find-by-id (connect!) :movie/id (:movie/id new-movie)))))
    (testing "when many-to-one"
      (d-h.entity/insert! (connect!) new-actor)
      (d-h.entity/upsert-foreign! (connect!)
                                  :city/id (-> fixtures.movies/data
                                               :city-2
                                               :city/id)
                                  :actor/city
                                  :actor/id (:actor/id new-actor))
      (is (= (assoc new-actor
               :actor/city (:city-2 fixtures.movies/data))
             (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor) '[* {:actor/city [*]}])))
      (d-h.entity/upsert-foreign! (connect!)
                                  :city/id (-> fixtures.movies/data
                                               :city-1
                                               :city/id)
                                  :actor/city
                                  :actor/id (:actor/id new-actor))
      (is (= (assoc new-actor
               :actor/city (:city-1 fixtures.movies/data))
             (d-h.entity/find-by-id (connect!) :actor/id (:actor/id new-actor) '[* {:actor/city [*]}]))))))

(deftest check-unique-test
  (testing "when unique"
    (is (false? (d-h.entity/check-unique! (connect!) :city/id :city/id (-> fixtures.movies/data :city-2 :city/id) :city/name "Havana"))))
  (testing "when non unique"
    (is (true? (d-h.entity/check-unique! (connect!) :city/id :city/id (-> fixtures.movies/data :city-2 :city/id) :city/name "Kutna Hora"))))
  (testing "when nothing is passed"
    (is (true? (d-h.entity/check-unique! (connect!) :city/id)))))

(deftest check-unique-custom-test
  (testing "when unique"
    (is (false? (d-h.entity/check-unique-custom! (connect!) :actor/id '[[?e :actor/name ?_0] [?e :actor/city ?c] [?c :city/name ?_1]] "Lenin" "Leningrad"))))
  (testing "when non unique"
    (is (true? (d-h.entity/check-unique-custom! (connect!) :actor/id '[[?e :actor/name ?_0] [?e :actor/city ?c] [?c :city/name ?_1]] "Lenin" "Kutna Hora"))))
  (testing "when nothing is passed"
    (is (true? (d-h.entity/check-unique-custom! (connect!) :actor/id '[[?e :actor/name ?_0] [?e :actor/city ?c] [?c :city/name ?_1]])))
    (is (true? (d-h.entity/check-unique-custom! (connect!) :actor/id '[] "Lenin")))))

(deftest delete-test
  (testing "when deleted"
    (d-h.entity/delete! (connect!) :city/id (-> fixtures.movies/data
                                                :city-1
                                                :city/id))
    (is (= (dissoc (:actor-1 fixtures.movies/data)
                   :actor/city)
           (d-h.entity/find-by-id (connect!) :actor/id (:actor/id (:actor-1 fixtures.movies/data)) '[* {:actor/city [*]}])))
    (is (nil? (d-h.entity/find-by-id (connect!) :city/id (:city/id (:city-1 fixtures.movies/data)))))))
