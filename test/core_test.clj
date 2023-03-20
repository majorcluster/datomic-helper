(ns core-test
  (:require [clojure.test :refer :all]
            [datomic-helper.fixtures.movies :as fixtures.movies]
            [datomic.api :as d]))

(defonce db-uri "datomic:mem:/movies")

(defn create-db! []
  (d/create-database db-uri))

(defn connect! []
  (d/connect db-uri))

(defn create-schema!
  [conn]
  (d/transact conn fixtures.movies/specs))

(defn insert-schema-samples!
  [conn]
  (fixtures.movies/insert-samples conn))

(defn erase-db!
  "test use only!!!"
  []
  (println "ERASING DB!!!!!!!")
  (d/delete-database db-uri))

(defn start-db
  []
  (create-db!)
  (let [conn (connect!)]
    (create-schema! conn)
    (insert-schema-samples! conn)))
(defn setup
  []
  (start-db))

(defn teardown
  []
  (erase-db!))

(defn test-fixture [f]
  (setup)
  (f)
  (teardown))
