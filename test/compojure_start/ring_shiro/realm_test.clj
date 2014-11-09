(ns compojure-start.ring-shiro.realm-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [compojure-start.cljcommon
             [clj-util :as clj-util]
             [app-settings :as app-settings]
             [db-util :as db-util]] :reload-all)
  (:import (java.util Date)))

(alias 'app-settings 'compojure-start.cljcommon.app-settings)

(app-settings/init)
(db-util/init)
(sec-util/init)


(defn fixture [f]
  (db-util/destroy-schema)
  (db-util/create-schema)
  (db-fixtures/create-sample-group4us 5)
  (db-fixtures/create-sample-users 5)
  (db-fixtures/create-sample-roles 5)
  (db-fixtures/create-sample-permissions 5)
  (f)
  (db-util/destroy-schema))

(use-fixtures :once fixture)

(defn- test-proxy
  []
  (proxy [com.m3958.lib.ringshiro.Parentc] []
    (doubleEcho [^String s]
      (str (.echo this s) (.echo this s)))))

(deftest proxy-test
  (let [parento (test-proxy)
        ds (.doubleEcho parento "a")]
    (is (= "aa" ds))))

(deftest user-hash
  (let [userh (sec-util/find-by :user :username (:username db-fixtures/userh))]
    (is (= 44 (count (:password userh))))
    (is (= 24 (count (:password_salt userh))))))

(deftest login-test
  (let [sub (sec-util/get-subject)
        username (:username db-fixtures/userh)
        pwd (:password db-fixtures/userh)]
    (is (= "un" username))
    (is (= "pwd" pwd))
    (-> sub (.login (sec-util/login-token username pwd)))))


(run-tests)
