(ns compojure-start.misc.coll-test
  (:require [clojure.test :refer :all]))


(deftest conj-t
  (is (= [1 2 3] (conj [1 2] 3)))
  (is (= [3 1 2] (conj (list 1 2) 3)))
  (is (= [1 2 [3 4]] (conj [1 2] [3 4])))
  (is (= [1 2 [3 4]] (conj [1 2] '(3 4)))))


(deftest flatten-t
  (is (= [1 2 3 4] (concat [1 2] [3 4] ())))
  (is (= [1 2 3 4] (apply concat '([1 2] [3 4] ())))))


