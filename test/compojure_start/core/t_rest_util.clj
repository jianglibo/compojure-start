(ns compojure-start.core.t-rest-util
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]
            [clojure.walk :as w]
            [ring.mock.request :as mock]
            [compojure-start.core.rest-util :as rest-util]))


(def ad (java.util.Date. 1418176804601))

(defn- v-f
  [k v]
  (condp = (type v)
    java.util.Date (.getTime v)
    v))

(def date-in-middle {:d [1 "a" :a ad]})

;clojure.lang.MapEntry

(fact "walk list"
      (w/walk #(+ % 10) identity [1 2]) => [11 12]
      (w/walk #(vec [(key %) (+ 10 (val %))]) (fn [r] (do (println r) r)) {:a 1 :b 2}) => {:a 11 :b 12}
      )

(fact "walk"
      (rest-util/to-json-walk [ad]) => [1418176804601]
      (rest-util/to-json-walk date-in-middle) => {:d [1 "a" :a 1418176804601]})

(fact "clojure data.json"
      (json/write-str []) => "[]"
      (json/write-str [:a]) => "[\"a\"]"
      (json/write-str {:d (java.util.Date.)} :value-fn v-f) => truthy
      (json/write-str (rest-util/to-json-walk date-in-middle) :value-fn v-f) => "{\"d\":[1,\"a\",\"a\",1418176804601]}"
      )

