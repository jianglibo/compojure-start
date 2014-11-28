(ns compojure-start.dutil.pressure-util
  (:require [clojure.tools.logging :as log]
            [clj-http.client :as client])
  (:import (java.util.concurrent Executors)))

(def cookie-store (clj-http.cookies/cookie-store))

(def cm (clj-http.conn-mgr/make-reusable-conn-manager {:timeout 2 :threads 3}))

(def home-page "http://oa.fh.gov.cn")

(def no-end-url "http://oa.fh.gov.cn/jwoa4share/jwoa4system/db_printview.nsf/PeoplePrintView?OpenAgent&infoid=mqhb_Info&path=jwoa4share/jwoa4app&dbname=db_mqhb.nsf")
(def verify-url "http://oa.fh.gov.cn/domcfg.nsf/LoginVerification?OpenAgent&Ran=0.7505126900505275&user=weisj&pd=680012")
(def index-url "http://oa.fh.gov.cn/index.nsf/Index?OpenForm")
(def login-url "http://oa.fh.gov.cn/names.nsf?Login")

(defn parse-response
  [avalue newres url]
  (let [len (Long/valueOf (-> newres :headers (get "Content-Length")))
        status (:status newres)
        request-time (:request-time newres)]
;    (log/info "len:" len)

;    (log/info "status type:" (type status))
;    (log/info "request-time type:" (type request-time))
    (conj avalue (-> {}
                     (assoc :status status)
                     (assoc :request-time request-time)
                     (assoc :len len)
                     (assoc :url url)))))


(defn start-user-agent
  [repeat-times & urls]
  (let [cs (clj-http.cookies/cookie-store)
        user-agent (agent [])]
    (log/info "current thread: " (-> (Thread/currentThread) .getId))
    (client/with-connection-pool {:timeout 5 :threads 4 :insecure? false :default-per-route 10}
      (client/get verify-url {:cookie-store cs :as "GB2312"})
      (client/post login-url  {:form-params {:fLoginVerification 1
                                           :Username "weisj"
                                           :Password "680012"
                                           :DBPath "/domcfg.nsf"
                                           :Path_Info "/index.nsf"
                                           :Path_Info_Decoded "/index.nsf"
                                           :SaveOptions 1
                                           :$PublicAccess 1}
                             :cookie-store cs})
      (dotimes [_ repeat-times]
        (doseq [url urls]
          (send-off user-agent parse-response (client/get url {:cookie-store cs}) url))))
    user-agent))

(defn report-result
  [rlist]
  (let [flatlist (flatten rlist)
        reqnum (count flatlist)]
    (log/info "total download: " (reduce (fn [m ite] (+ m (:len ite))) 0 flatlist))
    (log/info "average time: " (int (/ (reduce (fn [m ite] (+ m (:request-time ite))) 0 flatlist) reqnum)))
    (log/info "total request: " reqnum)
    (log/info "success: " (reduce (fn [m ite]
                                    (if (= 200 (:status ite))
                                      (inc m) m))
                                  0 flatlist))))


(defn start-req
  [unums rnums]
  (let [custom-pool (Executors/newFixedThreadPool unums)
        uagents (map (fn [_] (agent [])) (range unums))
        starttime (System/currentTimeMillis)]
    (doseq [ua uagents]
      (send-via custom-pool ua conj (start-user-agent rnums no-end-url)))
;return list of agent list.
;    (apply await uagents)
;    (report-result (map deref uagents))
;    (log/info "this test cost time: " (- (System/currentTimeMillis) starttime) "ms")
    (log/info uagents)
    ))

(start-req 20 5)
