(ns react-tutorial-om.core-test
  (:require [react-tutorial-om.core :refer :all]
            [clojure.test :refer :all]
            [clj-time.coerce :refer [from-date]]))

(deftest suggesting
  (testing "Normalise indexes"
    (is (= '(4 3 1 2) (normalise-indexes 10 2 [-2 -1 1 2])))
    (is (= '(7 8 5 6) (normalise-indexes 10 2 [7 8 10 11])))))

(deftest date-funcs
  (testing "recent"
    (let [now (from-date #inst "2014-03-01")]
      (are [expected input] (= expected (recent? input now))
           true "2014-02-20T17:27:07Z"
           false "2014-01-20T17:27:07Z"
           true "2014-02-20"
           true #inst "2014-02-20"
           false #inst "2014-01-20T17:27:07Z"
           false nil))))
