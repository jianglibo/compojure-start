(ns compojure-start.ring-shiro.core-test
  (:require [clojure.test :refer :all]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.ring-shiro.core :as ring-shiro-core])
  (:import (com.m3958.lib.ringshiro StateOb)
           (com.m3958.lib.ringshiro AcallableExecutor)))

(def ^:dynamic *shiro-response*)

(app-settings/init)
(db-util/init)
(sec-util/init)

;(defn before-test
;  []
;  (db-util/destroy-schema)
;  (db-util/create-schema)
;  (db-fixtures/create-sample-group4us 5)
;  (db-fixtures/create-sample-users 5)
;  (db-fixtures/create-sample-roles 5)
;  (db-fixtures/create-sample-permissions 5))
;
;(defn after-test
;  []
;  (db-util/destroy-schema))


(defn fixture [f]
  (db-util/destroy-schema)
  (db-util/create-schema)
  (db-fixtures/create-sample-group4us 5)
  (db-fixtures/create-sample-users 5)
  (db-fixtures/create-sample-roles 5)
  (db-fixtures/create-sample-permissions 5)
  (f)
  (db-util/destroy-schema))

(use-fixtures :once fixture)

(defn- sample-request
  []
  {:server-port 80
   :server-name "localhost"
   :remote-addr "127.0.0.1"
   :uri "/"
   :scheme "http"
   :request-method "get"
   :headers {:a 1}})

;they are from difference thread, so will not same.
(deftest sub-eq
  (is (not (= (ring-shiro-core/build-subject {}) (sec-util/get-subject))))
  (is (= (sec-util/get-subject) (sec-util/get-subject)))
  (is (not (= (ring-shiro-core/build-subject {}) (ring-shiro-core/build-subject {})))))


;when comming there is no session, handler do nothing about session, when leaving, it's no session too.
(defn simple-handler
  [request]
  {})

(defn- simple-app []
  (ring-shiro-core/wrap-shiro-test simple-handler))

(deftest simple-request
  (is (= {} (dissoc ((simple-app) {}) :subject))))

;when comming there is no session, handler create session, when leaving, there must has session.
(defn csession-handler
  [request]
  (db-fixtures/create-usera)
  (-> (sec-util/get-subject) (.login (sec-util/login-token
                                      (:username db-fixtures/userh)
                                      (:password db-fixtures/userh))))
  (db-fixtures/drop-usera)
  {})

(defn- csession-app []
  (ring-shiro-core/wrap-shiro-test csession-handler))

(deftest cssesion-request
  (binding [*shiro-response* ((csession-app) (sample-request))]
    (is (get-in *shiro-response* [:cookies :JSESSIONID]))
    (is (:subject *shiro-response*))
    (.logout (:subject *shiro-response*))))

;use logined sessionid, the next request's response should no :JSESSIONID key.
(deftest keep-session []
  (let [response ((csession-app) (sample-request))
        sessonId (get-in response [:cookies :JSESSIONID])
        new-response ((simple-app) {:cookies {:JSESSIONID sessonId}})]
    (is (not (get-in new-response [:cookies :JSESSIONID])))))

;(defn test-ns-hook
;  []
;  (before-test)
;  (sub-eq)
;  (simple-request)
;  (cssesion-request)
;  (keep-session)
;  (after-test))

(run-tests)

;((csession-app) (sample-request))
