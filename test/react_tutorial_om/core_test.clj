(ns react-tutorial-om.core-test
  (:require [react-tutorial-om.core :refer :all]
            [clojure.test :refer :all]))

(deftest suggesting
  (testing "Normalise indexes"
    (is (= '(4 3 1 2) (normalise-indexes 10 2 [-2 -1 1 2])))
    (is (= '(7 8 5 6) (normalise-indexes 10 2 [7 8 10 11])))))
