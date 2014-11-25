(ns compojure-start.ring-shiro.sec-util
  (:require [clojure.java.io :as io]
            [compojure-start.ring-shiro.const :as const]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [clojure.java.jdbc :as j]
            [compojure-start.cljcommon.db-util :as db-util]
            [clojure.string :as str])
  (:import (org.apache.shiro.mgt DefaultSecurityManager)
           (org.apache.shiro.session.mgt.eis EnterpriseCacheSessionDAO)
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
           (com.google.common.io BaseEncoding)
           (org.apache.shiro.crypto AesCipherService)
           (java.util UUID)))

(def cipher-service (doto
                      (AesCipherService.)
                      (.setKeySize 128)))

(def key-bytes (-> cipher-service
                   .generateNewKey
                   .getEncoded))

(def base64-url (BaseEncoding/base64Url))

(defn encrypt-str
  [^String raw-str]
  (let [bts (.getBytes raw-str)
        btsrc (.encrypt cipher-service bts key-bytes)]
    (.encode base64-url (.getBytes btsrc))))

(defn descrypt-str
  [^String base64str]
  (let [bts (.decode base64-url base64str)
        btsrc (.decrypt cipher-service bts key-bytes)]
    (String. (.getBytes btsrc))))


(def securitymanager-inited (ref false))

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

;SimpleAuthenticationInfo(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName)
(defn- proxy-realm [db-conn]
  (proxy [AuthorizingRealm] []
    (doGetAuthorizationInfo
     [principals]
     (when-not principals (throw (Throwable. "40002")))
     (let [principal (.getAvailablePrincipal this principals)
           allpermissions (map :pms (sec-db/allpermissions<-user (:id principal)))
           allroles (map :name (sec-db/allroles<-user (:id principal)))
           objectPermissions (map #(WildcardPermission. %1) allpermissions)]
       (doto (SimpleAuthorizationInfo.)
         (.setObjectPermissions (java.util.HashSet. objectPermissions))
         (.setRoles (java.util.HashSet. allroles)))))
    (doGetAuthenticationInfo
     [token]
     (let [username (.getUsername token)
           user (condp re-matches username
                  #"\d+" (sec-db/find-by :user :mobile username)
                  #".+@.+" (sec-db/find-by :user :email username)
                  (sec-db/find-by :user :username username))]
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

(init)


(comment
SELECT name FROM role
  INNER JOIN role_user ON role.id = role_user.role_id
  INNER JOIN user ON role_user.user_id = user.id
  WHERE user.id = 123

SELECT name FROM role
  WHERE id IN
  (SELECT role_id FROM role_user WHERE user_id = 123)
)
