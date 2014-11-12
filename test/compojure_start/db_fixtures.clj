(ns compojure-start.db-fixtures
  (:require [compojure-start.cljcommon
             [clj-util :as clj-util]
             [db-util :as db-util]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [clojure.set :as cset]
            [clojure.java.jdbc :as j] :reload-all))


(def userh {:username "un" :nickname "nn" :email "abc@gmail.com" :password "pwd"})
(def userh1 {:username "un1" :nickname "nna" :email "abc@gmail.com1" :password "pwd"})

(def rolea "rolea")
(def groupa "groupa")

(def group-tree '[b [c d]])

(defn- ramuserh []
  {:username (clj-util/random-str 8)
   :nickname "nn"
   :email (str (clj-util/random-str 8) "@gm.com")
   :password "pwd"
   })

(defn- ramuserhs [n]
  (conj (take n (repeatedly ramuserh)) userh))

(re-find #"[\d|.]+\s+msecs"
         (with-out-str
           (time (doseq [u (ramuserhs 1000)]
                   (:username u)))))

(defn create-sample-users [n]
  (let [uhs (ramuserhs n)]
    (doseq [uh uhs]
      (sec-util/create-user uh))))

(defn create-sample-roles [n]
  (let [rns (conj (take n (repeatedly (partial clj-util/random-str 8))) rolea)]
    (doseq [rn rns]
      (sec-util/create-role rn))))

(defn create-sample-group4us [n]
  (let [gns (conj (take n (repeatedly (partial clj-util/random-str 8))) groupa)]
    (doseq [gn gns]
      (sec-util/create-group4u gn))))

(defn- create-hash-set
  [kn values]
  (set (map (partial assoc {} kn) values)))

(def onames ["Article" "Section" "MailFolder"])
(def actions ["create" "read" "update" "delete"])

;(defn create-sample-permissions
;  [n]
;  (let [onamehs (create-hash-set :oname onames)
;        actionhs (create-hash-set :action actions)
;        idhs (create-hash-set :id (range n))
;        permhs (reduce cset/join [onamehs actionhs idhs])]
;    (doseq [ph permhs])
;    ))

(defn create-sample-permissions
  [n]
  (doseq [pmstr
          (for [oname onames action actions id (range n)]
            (str oname ":" action ":" id))]
    (sec-util/create-permission pmstr)))
