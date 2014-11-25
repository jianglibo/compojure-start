(ns compojure-start.misc.anonymous-fn-test
  (:require [clojure.test :refer :all]))


(deftest a-t
  (is (= '([1] [2] [3]) (map #(vector %) '(1 2 3))))
  (is (= '([1] [2] [3]) (map #(vector %1) '(1 2 3))))
  (is (= '({:a 1} {:b 2}) (map #(assoc {} %1 %2) '(:a :b) '(1 2))))
  (is (= [1 2 3] (vector 1 2 3))))

(run-tests)
