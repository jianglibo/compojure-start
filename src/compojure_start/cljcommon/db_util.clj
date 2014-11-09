(ns compojure-start.cljcommon.db-util
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure-start.cljcommon.clj-util :as clj-util]
            [compojure-start.cljcommon.app-settings :as app-settings]
            [clojure.java.shell :as shell]
            [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [clojure.math.numeric-tower :as math])
  (:import (java.nio.file Paths)
           (org.hsqldb.jdbc JDBCDriver)
           (com.mchange.v2.c3p0 ComboPooledDataSource)))

(def pooled-ds-ref (ref nil))

(defn db-conn [] @pooled-ds-ref)

(defn- pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defn cleanup
  []
  (.close (:datasource @pooled-ds-ref))
  (dosync (ref-set pooled-ds-ref nil)))


(defn db-meta []
  (j/with-db-metadata [md (db-conn)]
        (j/metadata-result (.getTables md nil nil nil (into-array ["TABLE"])))))

(defn table-meta [tablename]
  (j/with-db-metadata [md (db-conn)]
        (j/metadata-result (.getColumns md nil nil tablename nil))))

(defn all-table-names []
  (map :table_name (db-meta)))

(defn table-exist? [tablename]
  (some #(= (str/upper-case tablename) (str/upper-case %1))
    (map :table_name (db-meta))))

(defn column-exist? [tablename colname]
  (some #(= (str/upper-case tablename) (str/upper-case %1))
    (map :table_name (table-meta (str/upper-case (name colname))))))

(defn get-table-ddl [td]
  (apply j/create-table-ddl td))

(defn create-table
  "create one table."
  [td]
  (try
    (j/db-do-commands (db-conn) (apply j/create-table-ddl td))
    (catch Exception e)))


(defn create-schema []
  (doseq [sql (:create-tables-sql (app-settings/get-db-schema))]
    (j/db-do-commands (db-conn) sql)))

(defn destroy-schema []
  (doseq [sql (:drop-tables-sql (app-settings/get-db-schema))]
    (j/db-do-commands (db-conn) sql)))


(defn in-clause [ary]
  "generate sql in clause, (1, 2, 3)"
  (if-not ary
    "()"
    (if-not (coll? ary)
      (str "(" ary ")")
      (if (empty? ary)
        "()"
        (if (number? (first ary))
          (str "(" (str/join "," ary) ")")
          (str "(" (str/join "," (map #(str "'" %1 "'") ary)) ")"))))))

(defn init
  []
  (if @pooled-ds-ref
    (log/info "datasource already initialized.")
    (dosync
     (ref-set pooled-ds-ref (pool (app-settings/get-setting :db-spec))))))


;(jdbc/execute! db-spec ["UPDATE table SET col1 = NOW() WHERE id = ?" 77])
;(jdbc/db-do-commands db-spec "CREATE INDEX name_ix ON fruit ( name )")
