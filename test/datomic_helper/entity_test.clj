(ns datomic-helper.entity-test
  (:require [clojure.test :refer :all]
            [datomic-helper.entity :as d-h.entity]))

(deftest transform-out-test
  (testing "Not having db id, result keeps the same content"
    (is (= {:product/color "rot"}
           (d-h.entity/transform-out {:product/color "rot"})))
    (is (= {:product/color "rot"
            :product/id 1}
           (d-h.entity/transform-out {:product/color "rot"
                                      :product/id 1}))))
  (testing "Having db id it is removed"
    (is (= {:product/color "rot"}
           (d-h.entity/transform-out {:db/id 1
                                      :product/color "rot"})))
    (is (= {:product/color "rot"
            :product/id 1}
           (d-h.entity/transform-out {:db/id 93939383
                                      :product/color "rot"
                                      :product/id 1})))))
