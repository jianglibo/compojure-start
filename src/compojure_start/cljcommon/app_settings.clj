(ns compojure-start.cljcommon.app-settings
  (:require [compojure-start.cljcommon.clj-util :as clj-util]
            [clojure.tools.logging :as log]))

(def running-mode (ref nil))

(def settings (ref nil))

(def db-schema (ref nil))

(def ^:dynamic bingding-test (ref nil))

(defn get-setting
  [& args]
  (if-not @running-mode
    (throw (Throwable. "app setting not initialized.")))
  (or
   (get-in @settings (cons @running-mode args))
   (get-in @settings (cons :test args))))

(defn read-db-schema
  []
  (or
   (clj-util/read-res-edn (get-setting :db-schema))
   (clj-util/read-res-edn "db_schema.edn")
   (clj-util/read-res-edn "m3958/cljcommon/default/db_schema.edn")))

(defn init
  ([]
   (init nil :test))
  ([mode]
   (if (string? mode)
     (init mode :test))
     (init nil mode))
  ([setting-file mode]
   (if @running-mode
     (log/info "app already initialized.")
     (do
       (dosync
        (ref-set running-mode mode))
       (dosync
        (ref-set settings
                 (if setting-file
                   (clj-util/read-app-settings setting-file)
                   (clj-util/read-app-settings))))
       (dosync
        (ref-set db-schema (read-db-schema)))))))

(defn get-db-schema
  []
  @db-schema)


(defn get-binding-test
  []
  @bingding-test)
