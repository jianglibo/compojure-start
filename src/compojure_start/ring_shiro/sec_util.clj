(ns compojure-start.ring-shiro.sec-util
  (:require [clojure.java.io :as io]
            [compojure-start.ring-shiro.const :as const]
            [clojure.java.jdbc :as j]
            [compojure-start.cljcommon.db-util :as db-util]
            [clojure.string :as str])
  (:import (java.nio.file Paths)
           (org.apache.shiro.mgt DefaultSecurityManager)
           (org.apache.shiro.session.mgt.eis EnterpriseCacheSessionDAO)
           (com.m3958.lib.ringshiro WhatEverLoginRealm)
           (org.apache.shiro.subject Subject)
           (org.apache.shiro SecurityUtils)
           (org.apache.shiro.cache.ehcache EhCacheManager)
           (org.apache.shiro.session.mgt DefaultSessionManager)
           (org.apache.shiro.crypto.hash Sha256Hash)
           (org.apache.shiro.codec Base64)
           (org.apache.shiro.authc.credential HashedCredentialsMatcher)
           (org.apache.shiro.authc SimpleAuthenticationInfo UsernamePasswordToken)
           (org.apache.shiro.authz.permission WildcardPermission)
           (org.apache.shiro.authz SimpleAuthorizationInfo)
           (org.apache.shiro.realm AuthorizingRealm)
           (java.util UUID))
  (:gen-class))

(def securitymanager-inited (ref false))

(def unique-user-keys #{:id :email :username :mobile})

(def db-unique-keys {:user #{:id :email :username :mobile}
                     :group4u #{:id :name}
                     :role #{:id :name}})


(defn get-subject []
  (SecurityUtils/getSubject))

(defn has-login? []
  (.isAuthenticated (get-subject)))

(defn has-role? [rn]
  (.hasRole (get-subject) rn))

(defn has-any-role? [& roles]
  (.hasRoles (get-subject) roles))

(defn login-token [usr pwd]
  (UsernamePasswordToken. usr pwd))

(defn logout! []
  (.logout (get-subject)))

(defn get-salted-pair
  "get password salt and hashed password"
  [rawpwd]
  (let [salt (-> (org.apache.shiro.crypto.SecureRandomNumberGenerator.) (.nextBytes))
        hashedpwd (Sha256Hash. rawpwd salt const/PASSWORD_HASH_ITERATIONS)]
    [(.toBase64 hashedpwd) (.toBase64 salt)]))

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
     (j/insert! db-conn :user userh))))

(defn find-by
  "Find user by one unique field."
  [table fkey fval]
  (if-not (contains? (db-unique-keys table) fkey)
    (throw (Throwable. "10404")))

  (let [sql (str "SELECT * FROM " (name table) " WHERE " (name fkey) " = ?")
        rs (j/query (db-util/db-conn) [sql fval])]
    (if (empty? rs)
      nil
      (first rs))))

(defn create-role
  ([rn]
   (create-role (db-util/db-conn) rn))
  ([db-conn rn]
   (j/insert! db-conn :role {:name rn})))

(defn create-group4u
  ([gn]
   (create-group4u (db-util/db-conn) gn))
  ([db-conn gn]
   (j/insert! db-conn :group4u {:name gn})))

;newsletter:edit:13
(defn create-permission
  ([pms]
   (create-permission (db-util/db-conn) pms))
  ([db-conn pms]
   (j/insert! db-conn :permission {:pms pms})))

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
  (j/query (db-util/db-conn) [(str "SELECT id, pms FROM permission
                     INNER JOIN permission_role ON permission.id = permission_role.permission_id
                     INNER JOIN role ON role.id = permission_role.role_id
                     WHERE role.id IN " (db-util/in-clause role-ids))]))

(defn allpermissions<-user
  [user-id]
  (let [grps (group4us<-user user-id)
        rolesu (roles<-user user-id)
        rolesg (roles<-group4u (map :id grps))
        rids (map :id (set (concat rolesu rolesg)))
        permu (permissions<-user user-id)
        permr (permissions<-role rids)]
    (set (concat permu permr))))


;SimpleAuthenticationInfo(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName)
(defn- proxy-realm [db-conn]
  (proxy [AuthorizingRealm] []
    (doGetAuthorizationInfo
     [principals]
     (when-not principals (throw (Throwable. "40002")))
     (let [principal (.getAvailablePrincipal this principals)
           allpermissions (map :pms (allpermissions<-user (:id principal)))
           allroles (map :name (allroles<-user (:id principal)))
           objectPermissions (map #(WildcardPermission. %1) allpermissions)]
       (doto (SimpleAuthorizationInfo.)
         (.setObjectPermissions (java.util.HashSet. objectPermissions))
         (.setRoles (java.util.HashSet. allroles)))))
    (doGetAuthenticationInfo
     [token]
     (let [username (.getUsername token)
           user (condp re-matches username
                  #"\d+" (find-by :user :mobile username)
                  #".+@.+" (find-by :user :email username)
                  (find-by :user :username username))]
       (when-not user (throw (Throwable. "40001")))
       (let [^String password (:password user)
             ^String password-salt (:password_salt user)
             #^bytes dec-password-salt (Base64/decode password-salt)
             hashedCredentials (Sha256Hash/fromBase64String password)
             credentialsSalt (org.apache.shiro.util.ByteSource$Util/bytes dec-password-salt)
             principal (dissoc user :password :password_salt)]
         (SimpleAuthenticationInfo. principal hashedCredentials credentialsSalt const/USER_REALM_NAME))))))

(defn init
  []
  (if-not @securitymanager-inited
    (try
      (let [smgr (DefaultSecurityManager.)
            sessmgr (DefaultSessionManager.)
            sdao (EnterpriseCacheSessionDAO.)
            chmgr (EhCacheManager.)
            hc (doto
                 (HashedCredentialsMatcher. Sha256Hash/ALGORITHM_NAME)
                 (.setHashIterations const/PASSWORD_HASH_ITERATIONS))]
        (-> sessmgr (.setSessionDAO sdao))
        (doto smgr
;          (.setRealm (WhatEverLoginRealm.))
          (.setRealm (doto
                       (proxy-realm (db-util/db-conn))
                       (.setName const/USER_REALM_NAME)
                       (.setCredentialsMatcher hc)))
          (.setSessionManager sessmgr)
          (.setCacheManager chmgr))
        ;the order matters. .setCacheManager must the last call.
        (SecurityUtils/setSecurityManager smgr)
        (dosync (ref-set securitymanager-inited true)))
      (catch org.apache.shiro.cache.CacheException ce))))



(comment
SELECT name FROM role
  INNER JOIN role_user ON role.id = role_user.role_id
  INNER JOIN user ON role_user.user_id = user.id
  WHERE user.id = 123

SELECT name FROM role
  WHERE id IN
  (SELECT role_id FROM role_user WHERE user_id = 123)
)
