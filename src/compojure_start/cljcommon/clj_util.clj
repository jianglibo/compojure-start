(ns compojure-start.cljcommon.clj-util
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as shell])
  (:import (java.nio.file Paths)
           (java.io PushbackReader)
           (java.util UUID Set HashMap)))

(defn uuid []
  (-> (UUID/randomUUID) (str/replace  #"-" "")))

(defn is-windows
  []
  (re-find #"(?i)windows" (System/getProperty "os.name")))

(defn user-home
  []
  (System/getProperty "user.home"))

(defn working-dir []
  (System/getProperty "user.dir"))

(defn change-working-dir! [dir]
  (System/setProperty "user.dir" dir))

(defn filename
  [& pp]
  (if-not pp
    nil
    (->
     (Paths/get (first pp) (into-array String (rest pp)))
     (.normalize)
     (.toAbsolutePath)
     (.toString))))

(defn filename-in-user-home
  [& pp]
  (if pp
    (apply filename (user-home) pp)))

(defn filename-in-working-dir
  [& pp]
  (if pp
    (apply filename (working-dir) pp)))

(defn random-str [size]
  (str/join (take size (repeatedly #(rand-nth "0123456789abcdefghijklmnopqrstuvwxyz")))))

(defn read-res-edn [res]
  (let [reso (io/resource res)]
    (if-not reso
      nil
      (with-open [r (-> reso io/reader PushbackReader.)]
        (edn/read r)))))

(defn read-edn [& fns]
  (let [ffn (io/file (apply filename-in-working-dir fns))]
    (if-not (.exists ffn)
      nil
      (with-open [r (-> ffn io/reader PushbackReader.)]
        (edn/read r)))))

(defn read-app-settings
  ([]
   (or (read-res-edn "app_settings.edn") (read-res-edn "m3958/cljcommon/default/app_settings.edn")))
  ([fn]
   (or (read-res-edn fn) (read-res-edn "m3958/cljcommon/default/app_settings.edn"))))
