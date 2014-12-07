(ns compojure-start.misc.t-midje-nest
  (:require [midje.sweet :refer :all]))


(def orders (atom []))

(defn keep-order
  [od]
  (swap! orders conj od)
  (println od)
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


(against-background
 [(before :contents (keep-order "before-contents"))
  (after :contents (keep-order "after-contents"))]
 (fact
  (+ 1 1) => 2
  (+ 2 2) => 4
  (against-background [(before :facts (println "nest-against-background at end"))]))

 (against-background [(before :facts (println "nest-against-background at start"))]
   (fact
      (+ 3 3) => 6)))
