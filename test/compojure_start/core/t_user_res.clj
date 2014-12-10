(ns compojure-start.core.t-user-res
  (:require [clojure.test :refer :all]
            [compojure-start.core.user-res :as user-res]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pprint]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.cljcommon.db-util :as db-util]
            [compojure-start.core.rest-util :as rest-util]
            [compojure-start.core.handler :refer :all]))


(def users-mock-get (mock/request :get "/rest/user"))

(def user-mock-post (let [req (mock/request :post "/rest/user")]
                      (-> req
                          (mock/header "content-type" "application/json")
                          (mock/body (json/write-str db-fixtures/userh)))))

(against-background
 [(before :contents (do
                      (db-util/destroy-schema)
                      (db-util/create-schema)
                      (db-fixtures/create-sample-users 5)))
  (after :contents (db-util/destroy-schema))]

 (fact "user res exist."
       user-res/user  => truthy)

 (fact "get users"
       (let [resp (handler-for-test users-mock-get)]
         (pprint/pprint resp)
         (coll? resp) => truthy
         (:status resp) => 200))

 (fact "create user"
       (let [resp (handler-for-test user-mock-post)]
         (pprint/pprint resp)
         (coll? resp) => truthy
         (get-in resp [:headers "Location"]) => #"/rest/user/\d+$"
         (:status resp) => 303
         (let [resp (handler-for-test (mock/request :get (get-in resp [:headers "Location"])))]
           (pprint/pprint resp)
           (:status resp) => 200)))

 )
