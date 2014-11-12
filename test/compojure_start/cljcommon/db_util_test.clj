(ns compojure-start.cljcommon.db-util-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [compojure-start.cljcommon
             [clj-util :as clj-util]
             [app-settings :as app-settings]
             [db-util :as db-util]] :reload-all)
  (:import (java.util Date)))

(app-settings/init)
(db-util/init)

(def userh {:username "un" :nickname "nn" :email "abc@gmail.com" :password "pwd" :password_salt "salt"})
(def userh1 {:username "un1" :nickname "nn" :email "abc1@gmail.com" :password "pwd" :password_salt "salt"})

(defn fixture [f]
  (db-util/destroy-schema)
  (db-util/create-schema)
  (f)
  (db-util/destroy-schema))

(use-fixtures :each fixture)

(deftest dstype
  (is (coll? (db-util/db-conn)))
  (is (:datasource (db-util/db-conn))))

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


(deftest table-create-test
  (is (db-util/table-exist? "user")))

;return ({:id 0})
(deftest insert-user-test
  (is (let [ret (insert-user userh)]
        (and
         (seq? ret)
         (map? (first ret))
         (:id (first ret))
         (integer? (:id (first ret)))))))

;return [1]
(deftest raw-sql-test
  (is (let [ret (j/execute!
                 (db-util/db-conn)
                 ["INSERT INTO user
                  (username, nickname, email, password, password_salt, created_at, updated_at)
                  VALUES ('un', 'nn', 'abc@gm.com', '123', '123', NOW(), NOW())"])]
        (and
         (vector? ret)
         (integer? (ret 0))))))

(deftest not-null-test
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"NOT NULL check constraint"
       (insert-user (dissoc userh :username)))))

(deftest unique-test
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"unique constraint or index"
       (do
       (insert-user userh)
       (insert-user userh)))))

(deftest default-value-test
  (is (let [ret (insert-user userh)
            row (get-user-by-id (-> (first ret) :id))]
        (println row)
        (and
         row
         (= "NORMAL" (:status row))))))

;none exist user_id or role_id cann't insert
(deftest assign-role-err-test
  (is (thrown-with-msg?
       java.sql.SQLIntegrityConstraintViolationException
       #"foreign key no parent"
       (assign-role-to-user 10 10))))

(deftest assign-role-test
  (is (let [ur (insert-user userh)
            rr (insert-role {:name "arole"})
            uid (-> (first ur) :id)
            rid (-> (first rr) :id)]
        (assign-role-to-user uid rid))))

(deftest transactions-test
  (is (do
        (try
          (j/with-db-transaction [t-con (db-util/db-conn)]
            (dotimes [_ 2] (insert-user t-con userh)))
          (catch Exception e))
        (let [cr (j/query (db-util/db-conn) ["SELECT COUNT(*) AS user_count FROM user"])]
          (print "user number: ")
          (println cr)
          (= 0 (-> (first cr) :user_count)))
      )))

(deftest in-clause
  (is (= "()" (db-util/in-clause [])))
  (is (= "()" (db-util/in-clause nil)))
  (is (= "(1,2)" (db-util/in-clause [1 2])))
  (is (= "('1','2')" (db-util/in-clause ["1" "2"]))))

(run-tests)


