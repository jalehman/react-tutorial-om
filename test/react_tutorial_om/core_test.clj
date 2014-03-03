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

(deftest ranks
  (testing "calc-ranking-data"
    (let [matches [{:home "winners", :home_score 10, :away "losers", :away_score 0, :date nil}
                   {:home "arsenal", :home_score 3, :away "chelsea", :away_score 0, :date nil}
                   {:home "arsenal", :home_score 2, :away "chelsea", :away_score 0, :date nil}]]
      (is (= [{:loses 0, :draw 0, :wins 2, :rank 1, :team "arsenal", :ranking 1230.53, :rd nil, :round nil}
              {:loses 0, :draw 0, :wins 1, :rank 2, :team "winners", :ranking 1216.0, :rd nil, :round nil}
              {:loses 1, :draw 0, :wins 0, :rank 3, :team "losers", :ranking 1184.0, :rd nil, :round nil}
              {:loses 2, :draw 0, :wins 0, :rank 4, :team "chelsea", :ranking 1169.47, :rd nil, :round nil}]
             (calc-ranking-data matches))))))
