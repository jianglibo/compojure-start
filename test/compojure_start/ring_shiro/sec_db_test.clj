(ns compojure-start.ring-shiro.sec-db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [midje.sweet :refer :all]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.cljcommon.clj-util :as clj-util]
            [compojure-start.cljcommon.app-settings :as app-settings]
            [compojure-start.cljcommon.db-util :as db-util] :reload-all)
  (:import (java.util Date)
           (org.apache.shiro.subject Subject)
           (org.apache.shiro SecurityUtils)))


(background (before :contents (do (db-util/destroy-schema) (db-util/create-schema)))
            (after :contents (db-util/destroy-schema)))

(fact "group4u tree"
      (let [ids (db-fixtures/create-group-tree)
            [a1 b1 c1 d1 e1] ids
            [a b c d e] (map #(sec-db/find-by :group4u :id %) ids)]
        (:parent_id a) => falsey
        (:gpath a) => falsey
        (:parent_id b) => a1
        (:gpath b) => (str "." a1 ".")
        (:parent_id c) => b1
        (:gpath c) => (str "." (clojure.string/join "." [a1 b1]) ".")
        (db-fixtures/drop-group-tree)))


(deftest group4u-tree1
  (let [ids (db-fixtures/create-group-tree)
        [a b c d e] ids]
  (is (thrown-with-msg?
       Throwable
       #"10405"
       (sec-db/drop-group4u d)))
  (db-fixtures/drop-group-tree)))

(fact "group4u tree get children"
      (let [ids (db-fixtures/create-group-tree)
            [a b c d e] ids
            tops (sec-db/get-children :group4u nil)
            acs (sec-db/get-children :group4u a)]
        ;plus pre added 5 samples.
        (count tops) => 6
        (count (filter #(= (:id %) a) tops)) => 1
        (count acs) => 1
        (:id (first acs)) => b
        (db-fixtures/drop-group-tree)))

(fact "group4u tree get descendants"
      (let [ids (db-fixtures/create-group-tree)
            [a b c d e] ids
            tops (sec-db/get-descendants :group4u nil)
            acs (sec-db/get-descendants :group4u a)]
        (count tops) => 0
        (count acs) => 4
        (db-fixtures/drop-group-tree)))


(fact "create schema and drop schema are equal"
      (count (:create-tables-sql (app-settings/get-db-schema))) => (count (:drop-tables-sql (app-settings/get-db-schema))))


(fact "get salted pair"
      (let [pair (sec-db/get-salted-pair "abc")]
        (pair 0) => truthly
        (pair 1) => truthly
        (count (pair 0)) => 44
        (count (.getBytes (pair 0))) => 44
        (count (pair 1)) => 24))

(fact "create a user"
  (let [dbres (sec-db/create-user  db-fixtures/userh1)
        uid (first dbres)
        userh (sec-db/find-by :user :id uid)]
    uid => truthly
    (:password_salt userh) => truthly))


(defn- ut [token]
  (condp re-matches token
    #"\d+" :mobile
    #".+@.+"  :email
    :username))

(fact "username guess"
      (ut "ji@cc.c") => :email
      (ut "1234456") => :mobile
      (ut "ji123") => :username)

(against-background
 [(before :conntents (db-fixtures/create-groupa))
  (after :contents (db-fixtures/drop-groupa))]
 (fact "group4u roles"
       (let [group4u (sec-db/find-by :group4u :name "groupa")
             group4u-id (:id group4u)
             roles (sec-db/roles<-group4u  group4u-id)]
         (db-util/in-clause group4u-id) => #"\(\d+\)"
         roles => ())))

(against-background
 [(before :conntents (do (db-fixtures/create-usera) (db-fixtures/create-groupa)))
  (after :contents (do (db-fixtures/drop-usera) (db-fixtures/drop-groupa)))]
 (fact "assign role"
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
           (sec-db/permission->user pid user-id))
         (doseq [pid pidr]
           (sec-db/permission->role pid (first ridsu)))
         (doseq [rid ridsu]
           (sec-db/role->user rid user-id))
         (doseq [rid ridsg]
           (sec-db/role->group4u  rid group4u-id))
         (sec-db/user->group4u  user-id group4u-id)

         (count (sec-db/roles<-user  user-id)) => 2
         (count (sec-db/allroles<-user  user-id)) => 4
         (count (sec-db/allpermissions<-user  user-id)) => 8)))


