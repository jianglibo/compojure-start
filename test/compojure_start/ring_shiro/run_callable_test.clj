(ns compojure-start.ring-shiro.run-callable-test
  (:require [clojure.test :refer :all]
            [compojure-start.ring-shiro.rc :as rc]
            [clojure.template :as template])
  (:import (java.util.concurrent Callable)
           (java.lang Runnable)))


(defn demo-task []
         {})

(deftest run
    (is (nil? (.run demo-task)))
    (is (= {} (.call demo-task))))


(defn gf
  [subst]
  (template/apply-template '[x y] '(fn []
                                   (let [w 1
                                        x y]
                                   z)) subst))

(def afunc (eval (gf '[z (inc w)])))

(deftest tmp-func
  (eval (gf '[z (inc w)]))
  (is (afunc)))


(deftest defdef
  (rc/reset-def 1)
  (is (= 1 rc/v))
  (rc/reset-def 5)
  (is (= 5 rc/v))
  (rc/reset-def 6)
  (is (= 6 rc/v)))


(run-tests)
