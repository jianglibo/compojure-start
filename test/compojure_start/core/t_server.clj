(ns compojure-start.core.t-server
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [compojure-start.core.handler :refer :all]))

(def my-atom (atom 0))

(against-background
 [(before :facts (reset! my-atom 0))]
 (fact
  (swap! my-atom inc) => 1
  (swap! my-atom inc) => 2
  (swap! my-atom inc) => 3))
