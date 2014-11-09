(ns compojure-start.cljcommon.clj-util-test
  (:require [clojure.test :refer :all]
            [compojure-start.cljcommon.clj-util :refer :all]))

(defn hashf
  [{:keys [:dbpath :user :password]}]
  [dbpath user password])


(deftest hash-para-test
  (testing "hashf receive one parameter."
  (is (hashf {:dbpath "dp" :user "us" :password "pw"}))))


(deftest hash-para-test1
  (is (thrown-with-msg?
       clojure.lang.ArityException
       #"Wrong number of args"
       (hashf :dbpath "dp" :user "us" :password "pw"))))

(run-tests)
