(ns compojure-start.dutil.pressure-util
  (:require [clojure.tools.logging :as log]
            [clojure.string :as cstr]
            [clojure.pprint :as pprint]
            [clj-http.client :as client])
  (:import (java.util.concurrent Executors)))


(defn random-str [size]
  (cstr/join (take size (repeatedly #(rand-nth "0123456789abcdefghijklmnopqrstuvwxyz")))))

(defn save-response
  [result-atom newres url]
  (let [len (if-let [hlen (-> newres :headers (get "Content-Length"))]
              (Long/valueOf hlen)
              (count (:body newres)))
        status (:status newres)
        request-time (:request-time newres)]
;    (log/info "len:" len) (log/info "status type:" (type status)) (log/info "request-time type:" (type request-time))
    (swap! result-atom (fn [av r]
                         (let [len (count (av url))]
                           (assoc-in av [url len] r)))
           (-> {}
               (assoc :status status)
               (assoc :request-time request-time)
               (assoc :len len)))))

(defn- fetch-urls
  "fetch list of urls"
  [result-atom cs urls]
  (let [nurls (map #(cstr/replace % "{{rand}}" (random-str 8)) urls)]
    (doall
     (map (fn [nurl url]
            (save-response result-atom (try (client/get nurl {:cookie-store cs :socket-timeout 3000 :conn-timeout 3000})
                                         (catch Exception e (condp re-find (.getMessage e)
                                                              #"time out" {:status -100 :request-time 0}
                                                              {:status -1 :request-time 0}
                                                              ))) url))
            nurls urls))))

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

(defn do-login
  [userdefs]
  (doseq [ud userdefs]
    (if-let [lgf (first ud)]
      (lgf (last ud)))))

(defn prepare-users
  [result-atom userdefs]
  (doall (map (fn [ud] (let [urls (ud 1)]
                         (doall (map #(swap! result-atom assoc % []) urls))
                        )) userdefs))
  (let [nuserdefs (map (fn [ud]
                         (conj ud (clj-http.cookies/cookie-store)))
                       userdefs)
        users (apply concat (map #(repeat (% 2) %) nuserdefs))]
    (swap! result-atom assoc :thread-ids #{})
    (do-login nuserdefs)
    users))

(defn do-requests
  "this is a template, you can define other users too.it return a list of response list"
  [result-atom cs urls repeat-times]
  (client/with-connection-pool {:timeout 5 :threads 1 :insecure? false :default-per-route 10}
    (swap! result-atom (fn [av tid]
                         (let [tids (:thread-ids av)]
                           (assoc av :thread-ids (conj tids tid))))
                         (-> (Thread/currentThread) .getId))
    (dotimes [_ repeat-times]
      (fetch-urls result-atom cs urls))))

(defn benchmark
  "How many user repeat how many times.
  userdefs: [login-fn urls num rpt] [login-fn urls num rpt]
  user: [nil urls 10 10 #<BasicCookieStore []>]"
  [& userdefs]
  (let [result-atom (atom {})
        users (prepare-users result-atom userdefs)
        starttime (System/currentTimeMillis)
        futures (map #(future-call (partial do-requests result-atom (last %) (second %) (% 3))) users)]
    (doall futures)
    (doall (map deref futures))
    (log/info (with-out-str (pprint/pprint @result-atom)))
    (log/info "total time costs: " (- (System/currentTimeMillis) starttime))
    result-atom))



(defn oa-user-login
  [cs]
  (let [verify-url "http://oa.fh.gov.cn/domcfg.nsf/LoginVerification?OpenAgent&Ran=0.7505126900505275&user=weisj&pd=680012"
        login-url "http://oa.fh.gov.cn/names.nsf?Login"]
    (client/get verify-url {:cookie-store cs :as "GB2312"})
    (client/post login-url {:form-params {:fLoginVerification 1
                                          :Username "weisj"
                                          :Password "680012"
                                          :DBPath "/domcfg.nsf"
                                          :Path_Info "/index.nsf"
                                          :Path_Info_Decoded "/index.nsf"
                                          :SaveOptions 1
                                          :$PublicAccess 1}
                            :cookie-store cs})))

(def oa-user-request-urls
  ["http://oa.fh.gov.cn/jwoa4share/jwoa4system/db_printview.nsf/PeoplePrintView?OpenAgent&infoid=mqhb_Info&path=jwoa4share/jwoa4app&dbname=db_mqhb.nsf"
   "http://oa.fh.gov.cn/jwoa4share/jwoa4app/db_mqhb.nsf/TopBottomFrameSetWin?OpenForm&path=jwoa4share/jwoa4app/db_mqhb.nsf&fTitle=%E6%B0%91%E6%83%85%E4%BC%9A%E5%8A%9E&RndStr={{rand}}"
   "http://oa.fh.gov.cn/jwoa4share/jwoa4system/db_publicaffair.nsf/Toppic?OpenForm&RndStr={{rand}}"])



(benchmark [oa-user-login oa-user-request-urls 2 1])



;(defn prepare-users
;  [userdefs]
;  (let [nuserdefs (map (fn [ud]
;                         (conj ud (clj-http.cookies/cookie-store)))
;                       userdefs)
;        tnum (reduce #(+ %1 (%2 2)) 0 nuserdefs)
;        custom-pool (Executors/newFixedThreadPool tnum)
;        uagents (map #(agent %) (repeat tnum {}))
;        users (apply concat (map #(repeat (% 2) %) nuserdefs))]
;;    (clojure.core/set-agent-send-off-executor! (Executors/newFixedThreadPool tnum))
;    (do-login nuserdefs)
;    [custom-pool uagents users]))

;(defn benchmark
;  "How many user repeat how many times.
;  userdefs: [login-fn request-fn num rpt] [login-fn request-fn num rpt]
;  user: [nil baidu-user-request 10 10 #<BasicCookieStore []>]"
;  [& userdefs]
;  (let [[custom-pool uagents users] (prepare-users userdefs)
;        starttime (System/currentTimeMillis)]
;    (apply await (doall (map (fn [ua user]
;                  (send-off ua
;                     (fn [_ res] res)
;                     ((second user) (last user) (user 3)))) uagents users)))
;    (report-result (map deref uagents))
;    (log/info "total time costs: " (- (System/currentTimeMillis) starttime))))

