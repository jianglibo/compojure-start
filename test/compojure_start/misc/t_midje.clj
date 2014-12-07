(ns compojure-start.misc.t-midje
  (:require [midje.sweet :refer :all]))


(def orders (atom []))

(defn keep-order
  [od]
  (swap! orders conj od)
  (if (= @orders ["before-contents"
                  "before-facts"
                  "before-checks"
                  "after-checks"
                  "before-checks"
                  "after-checks"
                  "after-facts"
                  "before-facts"
                  "before-checks"
                  "after-checks"
                  "after-facts"
                  "after-contents"])
    (println "midje against-background ok!")))


(against-background [
                     (before :contents (keep-order "before-contents")) (before :facts (keep-order "before-facts")) (before :checks (keep-order "before-checks"))
                     (after :contents (keep-order "after-contents")) (after :facts (keep-order "after-facts")) (after :checks (keep-order "after-checks"))
                     ]
   ; A is evaluated here.
   ; B is evaluated here
   (fact
       ; C is evaluated here
       (+ 1 1) => 2
       ; C is evaluated here
       (+ 2 2) => 4)
   ; B is evaluated here
   (fact
      ; C is evaluated here
      (+ 3 3) => 6))
