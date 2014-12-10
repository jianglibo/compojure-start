(ns compojure-start.core.rest-util
  (:require [clojure.java.io :as io]
            [clojure.walk :as w]
            [clojure.data.json :as json])
  (:import (java.net URL)))


(defn- default-value-fn
  [k v]
  v)

(defn to-json-walk
  [data]
  (w/postwalk #(condp contains? (type %) #{java.util.Date java.sql.Date java.sql.Timestamp} (.getTime %) %) data))

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body :encoding "UTF-8")))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json
  [context data-key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (w/keywordize-keys (json/read-str body))]
          [false {data-key data}])
        {:message "No body"})
      (catch Exception e
        (println (get-in context [:request :body]))
        {:message (format "IOException: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

;; a helper to create a absolute url for the entry with the given id
(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

