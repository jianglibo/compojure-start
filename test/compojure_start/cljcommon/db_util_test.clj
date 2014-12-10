(ns compojure-start.cljcommon.db-util-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [midje.sweet :refer :all]
            [compojure-start.cljcommon
             [clj-util :as clj-util]
             [app-settings :as app-settings]
             [db-util :as db-util]] :reload-all)
  (:import (java.util Date)))

(def userh {:username "un" :nickname "nn" :email "abc@gmail.com" :password "pwd" :password_salt "salt"})
(def userh1 {:username "un1" :nickname "nn" :email "abc1@gmail.com" :password "pwd" :password_salt "salt"})

(defn- insert-user
  ([userh]
     (j/insert! (db-util/db-conn) :user userh))
  ([t-con userh]
     (j/insert! t-con :user userh)))

(defn- get-user-by-id [uid]
  (let [rs (j/query (db-util/db-conn) ["SELECT * FROM user WHERE id = ?" uid])]
    (if (empty? rs)
      nil
      (first rs))))

(defn- insert-role [roleh]
    (j/insert! (db-util/db-conn) :role roleh))

(defn- get-role-by-id [rid]
  (let [rs (j/query (db-util/db-conn) ["SELECT * FROM role WHERE id = ?" rid])]
    (if (empty? rs)
      nil
      (first rs))))

(defn- assign-role-to-user [uid rid]
  (j/insert! (db-util/db-conn) :role_user {:role_id rid :user_id uid}))


(against-background
 [(before :contents (do
                      (db-util/destroy-schema)
                      (db-util/create-schema)))
  (after :contents (db-util/destroy-schema))]

(fact "dstype"
  (coll? (db-util/db-conn)) => truthy
  (:datasource (db-util/db-conn)) => truthy)


(fact "table-create-test"
  (db-util/table-exist? "user") => truthy)

;return ({:id 0})
(fact "insert-user-test"
  (let [ret (insert-user userh)]
        (and
         (seq? ret)
         (map? (first ret))
         (:id (first ret))
         (integer? (:id (first ret))))) => truthy
      (j/execute! (db-util/db-conn) ["DELETE FROM user"]))

;return [1]
(fact "raw-sql-test"
  (let [ret (j/execute!
                 (db-util/db-conn)
                 ["INSERT INTO user
                  (username, nickname, email, password, password_salt, created_at, updated_at)
                  VALUES ('un', 'nn', 'abc@gm.com', '123', '123', NOW(), NOW())"])]
        (and
         (vector? ret)
         (integer? (ret 0)))) => truthy)

;(fact "not-null-test"
;  (is (thrown-with-msg?
;       java.sql.SQLIntegrityConstraintViolationException
;       #"NOT NULL check constraint"
;       "a")))

(fact "cannot insert user has no username"
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"NOT NULL check constraint"
       (insert-user (dissoc userh :username)))))

(fact "unique-test"
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"unique constraint or index"
       (do
       (insert-user userh)
       (insert-user userh))))
      (j/execute! (db-util/db-conn) ["DELETE FROM user"]))

(fact "default-value-test"
  (let [ret (insert-user userh)
            row (get-user-by-id (-> (first ret) :id))]
        (and
         row
         (= "NORMAL" (:status row)))) => truthy
      (j/execute! (db-util/db-conn) ["DELETE FROM user"]))

;none exist user_id or role_id cann't insert
(fact "assign-role-err-test"
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"foreign key no parent"
       (assign-role-to-user 10 10))))

(fact "assign-role-test"
  (let [ur (insert-user userh)
            rr (insert-role {:name "arole"})
            uid (-> (first ur) :id)
            rid (-> (first rr) :id)]
        (assign-role-to-user uid rid)) => truthy
      (j/execute! (db-util/db-conn) ["DELETE FROM role_user"])
      (j/execute! (db-util/db-conn) ["DELETE FROM user"]))

(fact "transactions-test"
  (do
    (try
      (j/with-db-transaction [t-con (db-util/db-conn)]
        (dotimes [_ 2] (insert-user t-con userh)))
      (catch Exception e))
    (let [cr (j/query (db-util/db-conn) ["SELECT COUNT(*) AS user_count FROM user"])]
      (-> (first cr) :user_count) => 0))
      (j/execute! (db-util/db-conn) ["DELETE FROM user"]))

(fact "in-clause"
  (db-util/in-clause []) => falsey
  (db-util/in-clause nil) => falsey
  (db-util/in-clause [1 2]) => "(1,2)"
  (db-util/in-clause ["1" "2"]) => "('1','2')")

)
