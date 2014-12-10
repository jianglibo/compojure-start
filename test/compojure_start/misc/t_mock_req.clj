(ns compojure-start.misc.t-mock-req
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [compojure-start.core.rest-util :as rest-util]))


(against-background
 [(around :facts (let [req (mock/request :get "/user/1" {:a 1 :b " ?"})] ?form))]
 (facts "test mock request"
       (fact "get method"
             (:uri req) => "/user/1"
             (:body req) => nil
             (:query-string req) => "b=+%3F&a=1"
             (:request-method req) => :get
             (:headers req) => {"host" "localhost"})))


(against-background
 [(around :facts (let [req (->
                             (mock/request :get "/user/1" {:a 1 :b " ?"})
                             (mock/header "myhead" "myheadvalue"))] ?form))]
 (facts "test mock request"
       (fact "get method"
             (:headers req) => {"host" "localhost" "myhead" "myheadvalue"})))


;body-as-string can only read once!!!
(against-background
 [(around :facts (let [req (->
                             (mock/request :post "/user")
                             (mock/body (json/write-str {:a 1 :b " ?"})))
                       reqbody (:body req)] ?form))]

 (facts "test rest-util post"
       (fact "post method"
             (type reqbody) => java.io.ByteArrayInputStream
             (json/read-str "{\"b\":\" ?\",\"a\":1}") => {"a" 1 "b" " ?"}
             (rest-util/parse-json {:request req} :data-key) => [false {:data-key {:a 1, :b " ?"}}])))



