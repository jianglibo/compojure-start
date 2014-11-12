(ns compojure-start.db-fixtures
  (:require [compojure-start.cljcommon
             [clj-util :as clj-util]
             [db-util :as db-util]]
            [compojure-start.ring-shiro.sec-db :as sec-db]
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

(re-find #"[\d|.]+\s+msecs"
         (with-out-str
           (time (doseq [u (take 1000 (repeatedly ramuserh))]
                   (:username u)))))

(defn create-usera
  []
  (sec-db/create-user userh))

(defn drop-usera
  []
  (sec-db/drop-user (:id (sec-db/find-by :user :username "un"))))

(defn create-groupa
  []
  (sec-db/create-group4u groupa))

(defn drop-groupa
  []
  (sec-db/drop-group4u (:id (sec-db/find-by :group4u :name groupa))))

(defn create-group-tree
  []
  (let [a (sec-db/create-group4u "a")
        b (sec-db/create-group4u "b" (first a))
        c (sec-db/create-group4u "c" (first b))
        d (sec-db/create-group4u "d" (first c))
        e (sec-db/create-group4u "e" (first d))]
    (map first [a b c d e])))

(defn drop-group-tree
  []
  (let [ids (reverse (map
             #(:id (sec-db/find-by :group4u :name %))
             '("a" "b" "c" "d" "e")))]
    (doseq [id ids]
      (sec-db/drop-group4u id))))

(defn create-sample-users [n]
  (let [uhs (take n (repeatedly ramuserh))]
    (doseq [uh uhs]
      (sec-db/create-user uh))))

(defn create-sample-roles [n]
  (let [rns (take n (repeatedly (partial clj-util/random-str 8)))]
    (doseq [rn rns]
      (sec-db/create-role rn))))

(defn create-sample-group4us [n]
  (let [gns (take n (repeatedly (partial clj-util/random-str 8)))]
    (doseq [gn gns]
      (sec-db/create-group4u gn))))

(defn- create-hash-set
  [kn values]
  (set (map (partial assoc {} kn) values)))

(def onames ["Article" "Section" "MailFolder"])
(def actions ["create" "read" "update" "delete"])

(defn create-sample-permissions
  [n]
  (doseq [pmstr
          (for [oname onames action actions id (range n)]
            (str oname ":" action ":" id))]
    (sec-db/create-permission pmstr)))
