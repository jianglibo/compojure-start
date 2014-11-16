(ns compojure-start.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.core :refer [wrap-shiro]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(app-settings/init)
(db-util/init)
(sec-util/init)

(defroutes app-routes
;  (GET "/" [] "Hello World")
  (GET "/" [] (io/resource "public/index.html"))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults (wrap-shiro app-routes)
                 (-> (dissoc site-defaults :session)
                     (assoc :static {:resources "public"}))))

;wrap-defaults total has 18 wraps,
;site-defaults is an hash config, which default include: params,cookies,session,security,static,responses
;defroutes return an handler.
