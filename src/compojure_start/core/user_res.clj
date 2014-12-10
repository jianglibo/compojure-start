(ns compojure-start.core.user-res
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [liberator.core :refer [resource defresource log!]]
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
  :handle-ok (fn [_]
               (let [dd (rest-util/to-json-walk (sec-db/get-users))]
                 (println dd)))
  :post! (fn [ctx]
           {::id (sec-db/create-user (::data ctx))})
  ;; actually http requires absolute urls for redirect but let's
  ;; keep things simple.
  :post-redirect? (fn [ctx]
                    {:location (format "/rest/user/%s" (::id ctx))}))

(defresource user [id]
  :allowed-methods [:get :put :delete]
  :known-content-type? #(rest-util/check-content-type % ["application/json"])
  :exists? (fn [_]
             (let [u (sec-db/find-by :user :id id)]
                    (if-not (nil? u)
                      {::entry u})))
  :existed? (fn [_] (nil? (sec-db/find-by :user :id id)))
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (rest-util/to-json-walk (::entry ctx)))
  :delete! (fn [_] (sec-db/drop-user id))
  :malformed? #(rest-util/parse-json % ::data)
  :can-put-to-missing? false
  :put! #(+ 1 1)
  :new? (fn [_] (nil? (:a))))
