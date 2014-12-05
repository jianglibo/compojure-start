(ns compojure-start.core.user-res
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [liberator.core :refer [resource defresource]]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [clojure.java.io :as io]
            [compojure-start.core.rest-util :as rest-util]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.sec-util :as sec-util]))

;application/json
;application/xml
;application/xhtml+xml

(defresource users
  :available-media-types ["application/json"]
  :allowed-methods [:post :get]
  :known-content-type? #(rest-util/check-content-type % ["application/json"])
  :malformed? #(rest-util/parse-json % ::data)
  :handle-ok (fn [ctx]
               (format (str "<html>Post text/plain to this resource.<br>\n"
                             "There are %d posts at the moment.")
                        3))
  :post! (fn [ctx]
           (dosync
            (let [body (slurp (get-in ctx [:request :body]))
                  id   5]
              {::id id})))
  ;; actually http requires absolute urls for redirect but let's
  ;; keep things simple.
  :post-redirect? (fn [ctx] {:location (format "/postbox/%s" (::id ctx))}))

(defresource user [id]
  :allowed-methods [:get :put :delete]
  :known-content-type? #(rest-util/check-content-type % ["application/json"])
  :exists? (fn [_]
             (let [e (get @entries id)]
                    (if-not (nil? e)
                      {::entry e})))
  :existed? (fn [_] (nil? (get @entries id ::sentinel)))
  :available-media-types ["application/json"]
  :handle-ok ::entry
  :delete! (fn [_] (dosync (alter entries assoc id nil)))
  :malformed? #(parse-json % ::data)
  :can-put-to-missing? false
  :put! #(dosync (alter entries assoc id (::data %)))
  :new? (fn [_] (nil? (get @entries id ::sentinel))))
