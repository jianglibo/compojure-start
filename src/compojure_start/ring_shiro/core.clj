(ns compojure-start.ring-shiro.core
  "Ring middleware for apache shiro integration"
  (:require [compojure-start.ring-shiro.sec-util :as sec-util])
  (:import (org.apache.shiro.subject Subject))
  (:gen-class))

(defn- create-cookie [cvalue & {:keys [path domain max-age expires secure http-only] :or {max-age -1 secure false http-only true}}]
  {:value cvalue :path path :max-age max-age :expires expires :secure secure :http-only http-only})

(defn- update-session-last-access-time []
  (let [sub (sec-util/get-subject)]
    (if sub
      (if-let [session (.getSession sub false)]
        (try (.touch session) (catch Throwable t))))))

(defn build-subject
  "when comming build a subject, when leaving get a subject from SecurityUtil"
  [request]
  (.buildSubject
   (doto (org.apache.shiro.subject.Subject$Builder.)
     (.sessionId (get-in request [:cookies :JSESSIONID]))
     (.host (:server-name request)))))

(defn- build-callable
  [handler request]
  (do
    (update-session-last-access-time)
    (cast Callable (partial handler request))))

(defn shiro-body
  [handler request & {debug :debug}]
  (let [subject (build-subject request)
        session-before (-> subject (.getSession false))
        response (-> subject (.execute (build-callable handler request)))
        session-after (-> subject (.getSession false))]
    (if (and (not session-before) session-after)
      (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie (.getId session-after)))]
        (if debug
          (assoc new-response :subject subject)
          new-response))

      (if (and session-before (not session-after))
        (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie "" :http-only false :max-age 0))]
          (if debug
            (assoc new-response :subject subject)
            new-response))
        response))))

(defn wrap-shiro
  "Middleware that enable apache shiro"
  [handler & {debug :debug}]
  (fn [request]
    (shiro-body handler request :debug debug)))

;(-> (build-subject {}) (.login (sec-util/login-token "a" "b")))
;(-> (sec-util/get-subject) (.getSession) (.getId))
