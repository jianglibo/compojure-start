(ns compojure-start.misc.t-midje-bk
  (:require [midje.sweet :refer :all]))

;order is:
;before-content
;after-contents
;before-facts
;before-checks
;after-checks
;before-checks
;after-checks
;after-facts
;before-facts
;before-checks
;after-checks
;after-facts

(def orders (atom []))

(defn keep-order
  [od]
  (swap! orders conj od)
  (if (= @orders ["before-contents"
                  "after-contents"
                  "before-facts"
                  "before-checks"
                  "after-checks"
                  "before-checks"
                  "after-checks"
                  "after-facts"
                  "before-facts"
                  "before-checks"
                  "after-checks"
                  "after-facts"])
    (println "midje background ok!")))

(background [
             (before :contents (keep-order "before-contents")) (before :facts (keep-order "before-facts")) (before :checks (keep-order "before-checks"))
             (after :contents (keep-order "after-contents")) (after :facts (keep-order "after-facts")) (after :checks (keep-order "after-checks"))])
   (fact
       (+ 1 1) => 2
       (+ 2 2) => 4)
   (fact
      (+ 3 3) => 6)
