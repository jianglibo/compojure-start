(ns compojure-start.ring-shiro.t-util
  (:require [compojure-start.cljcommon.clj-util :as clj-util]
            [compojure-start.cljcommon.db-util :as db-util])
  (:import [java.util.concurrent Executors]))

(def *pool* (Executors/newFixedThreadPool
             (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads! [f & {thread-count :threads
                       exec-count :times
                       :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit *pool* #(dotimes [_ exec-count] (f)))))

(def v (ref []))

(let [_ (dothreads! (fn [] (dosync (alter v conj 1))) :threads 5 :times 10)]
  (Thread/sleep 1000)
  (count @v))
