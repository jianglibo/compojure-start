(ns compojure-start.ring-shiro.misc-test
  (:require [clojure.test :refer :all]
            [clojure.zip :as zip]
            [compojure-start.ring-shiro.sec-util :as sec-util] :reload-all)
  (:import (com.m3958.lib.ringshiro StateOb)
           (com.m3958.lib.ringshiro AcallableExecutor)))

(defn- get-stateob
  []
  (let [ob (StateOb.)]
    (doto ob (.setAstring "i am string"))))

(deftest state-ob
  (is (= "i am string" (.getAstring (get-stateob)))))


(deftest a-callable
  (is (= [1 2] (-> (AcallableExecutor.) (.execute (fn [] [1 2]))))))


(deftest a-javalist
  (is (= ["a" "b" "c"] (-> (StateOb.) (.getStrlist)))))

(defn- set-javamap
  [cljmap]
  (doto
    (StateOb.)
    (.setStrmap cljmap)))

(deftest set-java-map
  (is (= {"a" "1"} (.getStrmap (set-javamap {"a" "1"})))))

(def xref (ref 0))

(map (partial + @xref) (range 10))

(def xrel #{ {:name "A"} {:name "B"} {:name "C"} {:name "D"}})
(def yrel #{ {:action "a"} {:action "b"} {:action "c"} {:action "d"}})

(defn- arg-arity
  [& arg]
  arg)

(deftest opt-extra
  (is (= ['(1 2) '(:db-conn 3)] (split-with #(not= :db-conn %) [1 2 :db-conn 3]))))

(deftest test-arg-arity
  (is (= clojure.lang.ArraySeq (type (arg-arity []))))
  (is (= '([]) (arg-arity [])))
  (is (= [1 2 3] (arg-arity 1 2 3)))
  (let [[a & b] (arg-arity 1 2 3)]
    (is (= 1 a))
    (is (= [2 3] b))))

(def for-count (ref 0))

(defn for-lazy
  []
  (for [x [1 2 3] y [4 5 6] z [7 8 9]]
    (do
      (dosync (alter for-count inc))
      (* x y z))))

;if you touch seq in for, then touch all item in that seq.
(deftest for-lazy?
  (dosync (ref-set for-count 0))
  (is (= 5 (count (take 5 (for-lazy)))))
  (is (= 6 @for-count))
  (dosync (ref-set for-count 0))
  (is (= 10 (count (take 10 (for-lazy)))))
  (is (= 12 @for-count))
  (dosync (ref-set for-count 0))
  (is (= 9 (count (take 9 (for-lazy)))))
  (is (= 9 @for-count))
  )

(deftest eval-test
  (let [x 6
        y 6]
    (is (thrown? Exception (eval '(* x y))))))

(deftest eval-test1
  (is (= 30 (eval '(let [x 5 y 6]
                     (* x y))))))



(run-tests)
