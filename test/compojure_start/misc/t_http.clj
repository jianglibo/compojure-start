(ns compojure-start.misc.t-http
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            ))

(def cookie-store (clj-http.cookies/cookie-store))

(def cm (clj-http.conn-mgr/make-reusable-conn-manager {:timeout 2 :threads 3}))

(def home-page "http://oa.fh.gov.cn")

(def no-end-url "http://oa.fh.gov.cn/jwoa4share/jwoa4system/db_printview.nsf/PeoplePrintView?OpenAgent&infoid=mqhb_Info&path=jwoa4share/jwoa4app&dbname=db_mqhb.nsf")
(def verify-url "http://oa.fh.gov.cn/domcfg.nsf/LoginVerification?OpenAgent&Ran=0.7505126900505275&user=weisj&pd=680012")
(def index-url "http://oa.fh.gov.cn/index.nsf/Index?OpenForm")
(def login-url "http://oa.fh.gov.cn/names.nsf?Login")


(defn login-in
  [cookie-store]
  (client/get verify-url {:cookie-store cookie-store :as "GB2312"})
  (client/post login-url  {:form-params {:fLoginVerification 1
                                           :Username "weisj"
                                           :Password "680012"
                                           :DBPath "/domcfg.nsf"
                                           :Path_Info "/index.nsf"
                                           :Path_Info_Decoded "/index.nsf"
                                           :SaveOptions 1
                                           :$PublicAccess 1}
                             :cookie-store cookie-store}))

(defn query-no-end
  [cookie-store]
  (client/with-connection-pool {:timeout 5 :threads 4 :insecure? false :default-per-route 10}
    (login-in cookie-store)
    [(client/get index-url {:cookie-store cookie-store :as "GB2312"})
    (client/get no-end-url {:cookie-store cookie-store})]))


(fact "login test"
      (let [[index-p no-end-p] (query-no-end cookie-store)]
        (log/info (:request-time no-end-p))
        (:body index-p) => #"待办事宜"))


;(against-background
; [(around :facts (let [req (mock/request :get "/user/1" {:a 1 :b " ?"})] ?form))]
; (facts "test mock request"
;       (fact "get method"
;             (:uri req) => "/user/1"
;             (:body req) => nil
;             (:query-string req) => "b=+%3F&a=1"
;             (:request-method req) => :get
;             (:headers req) => {"host" "localhost"}
;             )))
