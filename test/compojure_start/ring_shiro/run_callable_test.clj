(ns compojure-start.ring-shiro.run-callable-test
  (:require [clojure.test :refer :all])
  (:import (java.util.concurrent Callable)
           (java.lang Runnable)))


(defn demo-task []
         {})

(deftest run
  (dotimes [_ 10]
    (is (nil? (.run demo-task)))
    (is (= {} (.call demo-task)))))


(run-tests)
