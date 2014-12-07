(ns compojure-start.ring-shiro.sec-db
  (:require [clojure.java.io :as io]
            [compojure-start.ring-shiro.const :as const]
            [clojure.java.jdbc :as j]
            [compojure-start.cljcommon.db-util :as db-util]
            [clojure.string :as str])
  (:import (org.apache.shiro.crypto.hash Sha256Hash)))


(defn throw-404
  []
  (throw (Throwable. "10404")))

(defn- format-query
  ([table where]
   (let [tnstr (if (keyword? table)
                 (name table)
                 table)]
    (format "SELECT * FROM %s WHERE %s" tnstr where))))

(defn- format-find-by
  [table fkey]
  (if-not (contains? (const/db-unique-keys table) fkey)
    (throw-404))
  (str "SELECT * FROM " (name table) " WHERE " (name fkey) " = ?"))

(defn find-by
  "Find user by one unique field."
  [table fkey fval]
  (let [rs (j/query (db-util/db-conn) [(format-find-by table fkey) fval])]
    (if (empty? rs)
      nil
      (first rs))))

(defn get-salted-pair
  "get password salt and hashed password"
  [rawpwd]
  (let [salt (-> (org.apache.shiro.crypto.SecureRandomNumberGenerator.) (.nextBytes))
        hashedpwd (Sha256Hash. rawpwd salt const/PASSWORD_HASH_ITERATIONS)]
    [(.toBase64 hashedpwd) (.toBase64 salt)]))

(defn drop-entity-by
  ([table fkey fval]
   (drop-entity-by (db-util/db-conn) table fkey fval))
  ([conn table fkey fval]
   (if-not (contains? (const/db-unique-keys table) fkey)
     (throw-404))
   (j/delete! conn table [(str (name fkey) " = ?") fval])))

(defn create-user
  "Create a user."
  ([userh]
   (create-user (db-util/db-conn) userh))
  ([db-conn userh]
   (let [pwdkey :password
         saltkey :password_salt
         rawpwd (pwdkey userh)
         [saltedpwd salt] (get-salted-pair rawpwd)
         userh (-> userh (assoc pwdkey saltedpwd) (assoc saltkey salt))]
     (map :id
          (j/insert! db-conn :user userh)))))

(defn drop-user
  ([user-id]
   (drop-user (db-util/db-conn) user-id))
  ([conn user-id]
   (j/with-db-transaction [t-con conn]
     (j/delete! t-con :role_user ["user_id = ?" user-id])
     (j/delete! t-con :permission_user ["user_id = ?" user-id])
     (j/delete! t-con :group4u_user ["user_id = ?" user-id])
     (j/delete! t-con :user ["id = ?" user-id]))))

(defn drop-group4u
  ([group4u-id]
   (drop-group4u (db-util/db-conn) group4u-id))
  ([conn group4u-id]
   (try
     (j/with-db-transaction [t-con conn]
       (j/delete! t-con :group4u_user ["group4u_id = ?" group4u-id])
       (j/delete! t-con :group4u_role ["group4u_id = ?" group4u-id])
       (j/delete! t-con :group4u ["id = ?" group4u-id]))
     (catch Throwable _ (throw (Throwable. "10405"))))))

(defn drop-role
  ([role-id]
   (drop-role (db-util/db-conn) role-id))
  ([conn role-id]
   (j/with-db-transaction [t-con conn]
     (j/delete! t-con :role_user ["role_id = ?" role-id])
     (j/delete! t-con :permission_role ["role_id = ?" role-id])
     (j/delete! t-con :group4u_role ["role_id = ?" role-id])
     (j/delete! t-con :role ["id = ?" role-id]))))

(defn create-role
  ([rn]
   (create-role (db-util/db-conn) rn))
  ([db-conn rn]
   (map :id
        (j/insert! db-conn :role {:name rn}))))

(defn get-gpath
  [parent-id]
  (if parent-id
    (let [parent (find-by :group4u :id parent-id)]
      (if-not parent
        (throw-404))
      (str (or (:gpath parent) ".") (:id parent) "."))
    nil))

(defn create-group4u
  "return vector of one element."
 ([gn]
   (create-group4u (db-util/db-conn) gn nil))
  ([gn parent-id]
   (create-group4u (db-util/db-conn) gn parent-id))
  ([conn gn parent-id]
   (map :id
        (j/insert! conn :group4u {:name gn :parent_id parent-id :gpath (get-gpath parent-id)}))))

(defn get-children
  [table parent-id]
  (if parent-id
    (j/query (db-util/db-conn) [(format-query table "parent_id = ?") parent-id])
    (j/query (db-util/db-conn) [(format-query table "parent_id IS NULL")])))

(defn get-descendants
  [table ancestor-id]
  (if-not ancestor-id
    ()
    (j/query (db-util/db-conn) [(format-query table "gpath LIKE ?") (str "%." ancestor-id ".%")])))

;newsletter:edit:13
(defn create-permission
  ([pms]
   (create-permission (db-util/db-conn) pms))
  ([db-conn pms]
   (map :id
        (j/insert! db-conn :permission {:pms pms}))))

(defn role->user
  ([role-id user-id]
   (role->user (db-util/db-conn) role-id user-id))
  ([db-conn role-id user-id]
   (j/insert! db-conn :role_user {:role_id role-id :user_id user-id})))

(defn role->group4u
  ([role-id group4u-id]
   (role->group4u (db-util/db-conn) role-id group4u-id))
  ([db-conn role-id group4u-id]
   (j/insert! db-conn :group4u_role {:role_id role-id :group4u_id group4u-id})))

(defn user->group4u
  ([user-id group4u-id]
   (user->group4u (db-util/db-conn) user-id group4u-id))
  ([db-conn user-id group4u-id]
   (j/insert! db-conn :group4u_user {:user_id user-id :group4u_id group4u-id})))

(defn permission->user
  ([permission-id user-id]
   (permission->user (db-util/db-conn) permission-id user-id))
  ([db-conn permission-id user-id]
   (j/insert! db-conn :permission_user {:permission_id permission-id :user_id user-id})))

(defn permission->role
  ([permission-id role-id]
   (permission->role (db-util/db-conn) permission-id role-id))
  ([db-conn permission-id role-id]
   (j/insert! db-conn :permission_role {:permission_id permission-id :role_id role-id})))

(defn roles<-group4u
  [group4u-ids]
  (j/query (db-util/db-conn) [(str "SELECT id, name FROM role
                     INNER JOIN group4u_role ON role.id = group4u_role.role_id
                     INNER JOIN group4u ON group4u.id = group4u_role.group4u_id
                     WHERE group4u.id IN " (db-util/in-clause group4u-ids))]))

(defn users<-group4u
  [group4u-id]
  (j/query (db-util/db-conn) ["SELECT * FROM user
                     INNER JOIN group4u_user ON user.id = group4u_user.user_id
                     INNER JOIN group4u ON group4u_user.group4u_id = group4u.id
                     WHERE group4u.id = ?" group4u-id]))

(defn group4us<-user
  [user-id]
  (j/query (db-util/db-conn) ["SELECT * FROM group4u
                     INNER JOIN group4u_user ON group4u.id = group4u_user.group4u_id
                     INNER JOIN user ON group4u_user.user_id = user.id
                     WHERE user.id = ?" user-id]))


(defn roles<-user
  [user-id]
  (j/query (db-util/db-conn) ["SELECT id, name FROM role
                     INNER JOIN role_user ON role.id = role_user.role_id
                     INNER JOIN user ON role_user.user_id = user.id
                     WHERE user.id = ?" user-id]))

(defn allroles<-user
  [user-id]
  (let [grps (group4us<-user user-id)
        groles (roles<-group4u (map :id grps))
        uroles (roles<-user user-id)]
    (set (concat groles uroles))))

(defn permissions<-user
  [user-id]
  (j/query (db-util/db-conn) ["SELECT id, pms FROM permission
                     INNER JOIN permission_user ON permission.id = permission_user.permission_id
                     INNER JOIN user ON permission_user.user_id = user.id
                     WHERE user.id = ?" user-id]))

(defn permissions<-role
  [role-ids]
  (let [in-claus (db-util/in-clause role-ids)
        sql (str "SELECT id, pms FROM permission
                 INNER JOIN permission_role ON permission.id = permission_role.permission_id
                 INNER JOIN role ON role.id = permission_role.role_id
                 WHERE role.id IN " in-claus)]
    (if in-claus
      (j/query (db-util/db-conn) [sql]))))

(defn allpermissions<-user
  [user-id]
  (let [grps (group4us<-user user-id)
        rolesu (roles<-user user-id)
        rolesg (roles<-group4u (map :id grps))
        rids (map :id (set (concat rolesu rolesg)))
        permu (permissions<-user user-id)
        permr (permissions<-role rids)]
    (set (concat permu permr))))

(defn get-users
  [& {:keys [group-id email-like order-by p pp :or {p 1 pp 10 order-by "-created_at"}]}]
  (let [orby (if (= "-" (subs order-by 0 1))
               [(subs order-by 1) "DESC"]
               [order-by "ASC"])
        pi (if (number? p)
             p
             (Integer/valueOf p))
        ppi (if (number? pp)
              pp
              (Integer/valueOf pp))
        offset (* ppi (dec pi))
        sql (format "SELECT * FROM user ORDER BY ? %s LIMIT ? OFFSET ?" (first orby))]
    (j/query (db-util/db-conn) [sql (last orby) ppi offset])))
