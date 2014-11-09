(ns compojure-start.ring-shiro.core-test
  (:require [clojure.test :refer :all]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.ring-shiro.core :refer [wrap-shiro build-subject]])
  (:import (com.m3958.lib.ringshiro StateOb)
           (com.m3958.lib.ringshiro AcallableExecutor)))

(def ^:dynamic *shiro-respnse*)

(app-settings/init)
(db-util/init)
(sec-util/init)


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
  {:server-port 80, :server-name "localhost", :remote-addr "127.0.0.1", :uri "/" :scheme "http", :request-method "get", :headers {:a 1}})

;they are from difference thread, so will not same.
(deftest sub-eq
  (is (not (= (build-subject {}) (sec-util/get-subject)))))

;when comming there is no session, handler do nothing about session, when leaving, it's no session too.
(defn simple-handler
  [request]
  {})

(defn- simple-app []
  (wrap-shiro simple-handler :debug true))

(deftest simple-request
  (is (= {} ((simple-app) {}))))

;when comming there is no session, handler create session, when leaving, there must has session.
(defn csession-handler [request]
  (-> (sec-util/get-subject) (.login (sec-util/login-token (:username db-fixtures/userh) (:password db-fixtures/userh))))
  {})

(defn- csession-app []
  (wrap-shiro csession-handler :debug true))

(deftest cssesion-request
  (binding [*shiro-respnse* ((csession-app) (sample-request))]
    (is (get-in *shiro-respnse* [:cookies :JSESSIONID]))
    (is (:subject *shiro-respnse*))
    (is (do (.logout (:subject *shiro-respnse*)) true))))

;use logined sessionid, the next request's response should no :JSESSIONID key.
(deftest keep-session []
  (let [ser ((csession-app) (sample-request))
        sessonId (get-in ser [:cookies :JSESSIONID])
        new-ser ((simple-app) {:cookies {:JSESSIONID sessonId}})]
    (is (not (get-in new-ser [:cookies :JSESSIONID])))))

(run-tests)

;((csession-app) (sample-request))
