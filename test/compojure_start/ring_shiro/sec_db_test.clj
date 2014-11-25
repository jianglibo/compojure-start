(ns compojure-start.ring-shiro.sec-db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.cljcommon.clj-util :as clj-util]
            [compojure-start.cljcommon.app-settings :as app-settings]
            [compojure-start.cljcommon.db-util :as db-util] :reload-all)
  (:import (java.util Date)
           (org.apache.shiro.subject Subject)
           (org.apache.shiro SecurityUtils)))


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

(deftest group4u-tree
  (let [ids (db-fixtures/create-group-tree)
        [a1 b1 c1 d1 e1] ids
        [a b c d e] (map #(sec-db/find-by :group4u :id %) ids)]
    (is (nil? (:parent_id a)))
    (is (nil? (:gpath a)))
    (is (= a1 (:parent_id b)))
    (is (= (str "." a1 ".") (:gpath b)))
    (is (= b1 (:parent_id c)))
    (is (= (str "." (clojure.string/join "." [a1 b1]) ".") (:gpath c))))
  (db-fixtures/drop-group-tree))


(deftest group4u-tree1
  (let [ids (db-fixtures/create-group-tree)
        [a b c d e] ids]
  (is (thrown-with-msg?
       Throwable
       #"10405"
       (sec-db/drop-group4u d)))
  (db-fixtures/drop-group-tree)))

(deftest group4u-get-child
  (let [ids (db-fixtures/create-group-tree)
        [a b c d e] ids
        tops (sec-db/get-children :group4u nil)
        acs (sec-db/get-children :group4u a)]
    ;plus pre added 5 samples.
    (is (= 6 (count tops)))
    (is (= 1 (count (filter #(= (:id %) a) tops))))
    (is (= 1 (count acs)))
    (is (= b (:id (first acs))))
  (db-fixtures/drop-group-tree)))

(deftest group4u-get-descendants
  (let [ids (db-fixtures/create-group-tree)
        [a b c d e] ids
        tops (sec-db/get-descendants :group4u nil)
        acs (sec-db/get-descendants :group4u a)]
    (is (= 0 (count tops)))
    (is (= 4 (count acs)))
  (db-fixtures/drop-group-tree)))


(deftest schema-equal
  (is (= (count (:create-tables-sql (app-settings/get-db-schema)))
         (count (:drop-tables-sql (app-settings/get-db-schema)))) "create table and drop table are equal"))


(deftest get-salted-pair-test
  (let [pair (sec-db/get-salted-pair "abc")]
    (is (pair 0))
    (is (pair 1))
    (is (= 44 (count (pair 0))))
    (is (= 44 (count (.getBytes (pair 0)))) "bytes length")
    (is (= 24 (count (pair 1))))
    ))

(deftest create-user
  "create a new user, new created user must has password_salt."
  (let [dbres (sec-db/create-user  db-fixtures/userh1)
        uid (first dbres)
        userh (sec-db/find-by :user :id uid)]
    (is uid "user id should return.")
;    (println userh)
    (is (:password_salt userh))))


(defn- ut [token]
  (condp re-matches token
    #"\d+" :mobile
    #".+@.+"  :email
    :username))

(deftest username-test
  "Test username type."
  (is (= :email (ut "ji@cc.c")))
  (is (= :mobile (ut "1234456")))
  (is (= :username (ut "ji123"))))

(deftest group4u-roles
  (db-fixtures/create-groupa)
  (let [group4u (sec-db/find-by :group4u :name "groupa")
        group4u-id (:id group4u)]
    (is (re-find #"\(\d+\)"  (db-util/in-clause group4u-id))))
  (db-fixtures/drop-groupa))

(deftest group4u-roles1
  (db-fixtures/create-groupa)
  (let [group4u (sec-db/find-by :group4u :name "groupa")
        group4u-id (:id group4u)
        roles (sec-db/roles<-group4u  group4u-id)]
    (is (= () roles) "empty roles"))
  (db-fixtures/drop-groupa))


(deftest assign-role-test
  (db-fixtures/create-usera)
  (db-fixtures/create-groupa)
  (let [user (sec-db/find-by :user :username "un")
        user-id (:id user)
        group4u (sec-db/find-by :group4u :name "groupa")
        group4u-id (:id group4u)
        roles (j/query (db-util/db-conn) ["SELECT * FROM role"])
        perms (j/query (db-util/db-conn) ["SELECT * FROM permission"])
        pidu (take 2 (map :id perms))
        pidr (take-last 6 (map :id perms))
        ridsu (take 2 (map :id roles))
        ridsg (take 4 (map :id roles))]
    (doseq [pid pidu]
      (sec-db/permission->user  pid user-id))
    (doseq [pid pidr]
      (sec-db/permission->role  pid (first ridsu)))
    (doseq [rid ridsu]
      (sec-db/role->user  rid user-id))
    (doseq [rid ridsg]
      (sec-db/role->group4u  rid group4u-id))
    (let [aroles (sec-db/roles<-user  user-id)]
      (is (= 2 (count aroles))))
    (sec-db/user->group4u  user-id group4u-id)
    (let [aarole (sec-db/allroles<-user  user-id)]
;      (println aarole)
      (is (= 4 (count aarole))))
    (let [permissions (sec-db/allpermissions<-user  user-id)]
      (is (= 8 (count permissions)))))
  (db-fixtures/drop-usera)
  (db-fixtures/drop-groupa))


