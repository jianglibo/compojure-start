(ns compojure-start.ring-shiro.sec-util-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.cljcommon.clj-util :as clj-util]
            [compojure-start.cljcommon.app-settings :as app-settings]
            [compojure-start.cljcommon.db-util :as db-util] :reload-all)
  (:import (java.util Date)
           (org.apache.shiro.subject Subject)
           (org.apache.shiro SecurityUtils)))

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

(deftest schema-equal
  (is (= (count (:create-tables-sql (app-settings/get-db-schema)))
         (count (:drop-tables-sql (app-settings/get-db-schema)))) "create table and drop table are equal"))

(deftest security-mgr
  (let [smgr (SecurityUtils/getSecurityManager)
            sessmgr (.getSessionManager smgr)
            sdao (-> smgr (.getSessionManager) (.getSessionDAO))
            camgr (.getCacheManager smgr)]
         (is (= (type smgr) org.apache.shiro.mgt.DefaultSecurityManager))
         (is (= (type sessmgr) org.apache.shiro.session.mgt.DefaultSessionManager))
         (is (= (type sdao) org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO))
         (is (= (type camgr) org.apache.shiro.cache.ehcache.EhCacheManager))))

(deftest get-salted-pair-test
  (let [pair (sec-util/get-salted-pair "abc")]
    (is (pair 0))
    (is (pair 1))
    (is (= 44 (count (pair 0))))
    (is (= 44 (count (.getBytes (pair 0)))) "bytes length")
    (is (= 24 (count (pair 1))))
    ))

(deftest create-user
  "create a new user, new created user must has password_salt."
  (let [dbres (sec-util/create-user  db-fixtures/userh1)
        uidkey (-> dbres first keys first)
        uid (uidkey (first dbres))
        userh (sec-util/find-by :user :id uid)]
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
  (let [group4u (sec-util/find-by :group4u :name "groupa")
        group4u-id (:id group4u)]
    (is (= "(0)" (db-util/in-clause group4u-id)))))

(deftest group4u-roles1
  (let [group4u (sec-util/find-by :group4u :name "groupa")
        group4u-id (:id group4u)
        roles (sec-util/roles<-group4u  group4u-id)]
    (is (= () roles) "empty roles")))

(deftest assign-role-test
  (let [user (sec-util/find-by :user :username "un")
        user-id (:id user)
        group4u (sec-util/find-by :group4u :name "groupa")
        group4u-id (:id group4u)
        roles (j/query (db-util/db-conn) ["SELECT * FROM role"])
        perms (j/query (db-util/db-conn) ["SELECT * FROM permission"])
        pidu (take 2 (map :id perms))
        pidr (take-last 6 (map :id perms))
        ridsu (take 2 (map :id roles))
        ridsg (take 4 (map :id roles))]
    (doseq [pid pidu]
      (sec-util/permission->user  pid user-id))
    (doseq [pid pidr]
      (sec-util/permission->role  pid (first ridsu)))
    (doseq [rid ridsu]
      (sec-util/role->user  rid user-id))
    (doseq [rid ridsg]
      (sec-util/role->group4u  rid group4u-id))
    (let [aroles (sec-util/roles<-user  user-id)]
      (is (= 2 (count aroles))))
    (sec-util/user->group4u  user-id group4u-id)
    (let [aarole (sec-util/allroles<-user  user-id)]
;      (println aarole)
      (is (= 4 (count aarole))))
    (let [permissions (sec-util/allpermissions<-user  user-id)]
      (is (= 8 (count permissions))))))


(run-tests)

;CROSS TABLE Cartesian product
;(A) CROSS JOIN (B) => SELECT * FROM A,B
;not common used

;UNION JOIN, a table has 3 columns u v w , b table has 3 columns z y z
; not common used.

;JOIN ... ON Equijoin is the most commonly used type of join.
;SELECT a.*, b.* FROM a INNER JOIN b ON a.col_one = b.col_two

;SELECT * FROM a INNER JOIN b ON a.col_two = b.col_two

