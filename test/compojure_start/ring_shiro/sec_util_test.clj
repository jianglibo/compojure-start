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

(deftest security-mgr
  (let [smgr (SecurityUtils/getSecurityManager)
            sessmgr (.getSessionManager smgr)
            sdao (-> smgr (.getSessionManager) (.getSessionDAO))
            camgr (.getCacheManager smgr)]
         (is (= (type smgr) org.apache.shiro.mgt.DefaultSecurityManager))
         (is (= (type sessmgr) org.apache.shiro.session.mgt.DefaultSessionManager))
         (is (= (type sdao) org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO))
         (is (= (type camgr) org.apache.shiro.cache.ehcache.EhCacheManager))))



;CROSS TABLE Cartesian product
;(A) CROSS JOIN (B) => SELECT * FROM A,B
;not common used

;UNION JOIN, a table has 3 columns u v w , b table has 3 columns z y z
; not common used.

;JOIN ... ON Equijoin is the most commonly used type of join.
;SELECT a.*, b.* FROM a INNER JOIN b ON a.col_one = b.col_two

;SELECT * FROM a INNER JOIN b ON a.col_two = b.col_two

