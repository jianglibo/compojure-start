(ns compojure-start.db-fixtures
  (:require [compojure-start.cljcommon
             [clj-util :as clj-util]
             [db-util :as db-util]]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [clojure.zip :as z]
            [clojure.set :as cset]
            [clojure.template :as template]
            [clojure.java.jdbc :as j] :reload-all))


(def userh {:username "un" :nickname "nn" :email "abc@gmail.com" :password "pwd"})
(def userh1 {:username "un1" :nickname "nn1" :email "abc@gmail.com1" :password "pwd1"})

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
  (sec-db/create-group4u groupa nil))

(defn drop-groupa
  []
  (sec-db/drop-group4u (:id (sec-db/find-by :group4u :name groupa))))

(defn create-group-tree
  []
  (let [a (sec-db/create-group4u "a" nil)
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
      (sec-db/create-group4u gn nil))))

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

(def node-number (ref 1))

(map (fn [_] {:id (dosync (alter node-number inc)) :parent_id 5}) (range 3))

(z/zipper (fn [nd] (< @node-number 100))
          (fn [nd] (map
                    (fn [_] {:id (dosync (alter node-number inc)) :parent_id (:id nd)})
                    (range 3)))
          (fn [nd chs] chs)
          [{:id 0 :parent_id nil}])

(defn node-value
  [loc]
  (first (z/node loc)))

(defn make-node [loc]
  (z/append-child loc (z/make-node loc (z/node loc) [])))

(defn make-nodes [loc brh]
;  (count (take brh (iterate make-node loc))))
  (last (take brh (iterate make-node loc))))

(defn create-tree
  [total brh]
    (let [loc (z/vector-zip [])]
      (-> (z/append-child loc {:id 0 :parent_id nil})
          (make-nodes brh))))

(create-tree 100 5)

(def inc-start (ref 0))

(defn get-next []
  (dosync (alter inc-start inc)))

(defmacro bcf
  [n]
  (vec (map (fn [_] 'x) (range n)))
  )

(macroexpand '(bcf 5))
(let [x 6]
  (bcf 5))

(defmacro pointless [n] n)

(pointless (+ 3 5))

(defn- create-children
  [parentv n]
  (flatten (map
            (fn [pnd]
              (take n
                    (map
                     (fn [id] {:id id, :parent_id (:id pnd)})
                     (repeatedly get-next))))
            parentv)))

(defmacro tree-items
  [lev n]
  (let [llls (let [levs (map #(symbol (str "i" %)) (rest (range lev)))
                   levs1 (map #(symbol (str "i" %)) (drop-last (range lev)))
                   ctpl '(create-children x y)
                   tpl-seq (map #(vector %1 (template/apply-template '[x y] ctpl [%2 n])) levs levs1)]
               (vec (apply concat tpl-seq)))
        lresult (vec (map #(symbol (str "i" %)) (rest (range lev))))
        scaffold (template/apply-template '[x llls lresult]
                                          '(let [i0 (take x (map #(assoc {:parent_id nil} :id %) (repeatedly get-next)))
                                                 levels (let llls lresult)]
                                             (flatten [i0 levels]))
                                          [n llls lresult])]
    scaffold))

(macroexpand '(tree-items 5 10))

(defn tree-items-i
  [lev n]
  (let [lev (dec lev)
        rootn (take n (map #(assoc {:parent_id nil} :id %) (repeatedly get-next)))]
    (flatten (take lev (iterate #(create-children % n) rootn)))))

;(with-out-str
;  (time (count (tree-items 5 10))))
;
;(with-out-str
;  (time (count (tree-items-i 4 10))))

;(count (tree-items-i 4 10))
;
;(with-out-str
;  (time (count (tree-items 5 10))))

;(let* [i0 (take 10 (map (fn* [p1__60273#] (assoc {:parent_id nil} :id p1__60273#)) (repeatedly get-next)))
;       levels (let [i1 (create-children i0 10)
;                    i2 (create-children i1 10)
;                    i3 (create-children i2 10)
;                    i4 (create-children i3 10)]
;                [i1 i2 i3 i4])]
;      (flatten [i0 levels]))
