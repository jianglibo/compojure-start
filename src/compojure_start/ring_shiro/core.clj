(ns compojure-start.ring-shiro.core
  "Ring middleware for apache shiro integration"
  (:require [compojure-start.ring-shiro.sec-util :as sec-util])
  (:import (org.apache.shiro.subject Subject))
  (:gen-class))

(defn- create-cookie [cvalue & {:keys [path domain max-age expires secure http-only] :or {max-age -1 secure false http-only true}}]
  {:value cvalue :path path :max-age max-age :expires expires :secure secure :http-only http-only})

(defn- update-session-last-access-time
  []
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

(defn- reify-callable
  [handler request]
  (reify Callable
    (call [_] (handler request))))

(defn wrap-shiro
  "Middleware that enable apache shiro"
  [handler]
  (fn [request]
    (let [subject (build-subject request)
          session-before (-> subject (.getSession false))
          ^Callable calab (reify-callable handler request)
          response (-> subject (.execute calab))
          session-after (-> subject (.getSession false))]
      (if (and (not session-before) session-after)
        (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie (.getId session-after)))]
          new-response)

        (if (and session-before (not session-after))
          (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie "" :http-only false :max-age 0))]
            new-response)
          response)))))

(defn wrap-shiro-for-test
  "only difference is response has :subject key."
  [handler]
  (fn [request]
    (let [subject (build-subject request)
          session-before (-> subject (.getSession false))
          ^Callable calab (reify-callable handler request)
          response (-> subject (.execute calab))
          response (assoc response :subject subject)
          session-after (-> subject (.getSession false))]
      (if (and (not session-before) session-after)
        (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie (.getId session-after)))]
          new-response)

        (if (and session-before (not session-after))
          (let [new-response (assoc-in response [:cookies :JSESSIONID] (create-cookie "" :http-only false :max-age 0))]
            new-response)
          response)))))
