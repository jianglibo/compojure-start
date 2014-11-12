(ns compojure-start.ring-shiro.core-test
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

;(def dz (zip/vector-zip '[a [b [c d [e]]]]))

;(loop [loc dz]
;  (if (zip/end? loc)
;    (zip/root loc)
;    (recur
;     (do
;       (println (zip/path loc))
;       (zip/next loc)))))
