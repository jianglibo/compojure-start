(ns compojure-start.core.user-res
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [liberator.core :refer [resource defresource]]
            [clojure.java.io :as io]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.sec-util :as sec-util]))

;application/json
;application/xml
;application/xhtml+xml

(defresource user
        :allowed-methods [:post :get]
        :available-media-types ["application/json"]
        :handle-ok (fn [ctx]
                     (format  (str "<html>Post text/plain to this resource.<br>\n"
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
