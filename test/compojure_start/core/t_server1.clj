(ns compojure-start.core.t-server1
  (:require [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [midje.sweet :refer :all]
            [clj-http.client :as client]
            [ring.mock.request :as mock]
            [compojure-start.core.handler :refer :all]))

(def my-atom (atom 0))

(background (before :facts (reset! my-atom 0)))

(defn handler [request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello world"})

(def server (atom nil))

(defn start-server
  "Start a ring server and store a reference"
  []
  (swap! server
         (fn [_] (run-jetty handler {:port 3000 :join? false}))))

(defn stop-server
  []
  (.stop @server))

(background (before :contents (start-server))
            (after :contents (stop-server)))



(fact
  (swap! my-atom inc) => 1
  (swap! my-atom inc) => 2
  (swap! my-atom inc) => 3)

(fact "hello world is served at root"
  (let [response (client/get "http://localhost:3000")]
    (response :status) => 200
    (response :body) => "just testing"))
