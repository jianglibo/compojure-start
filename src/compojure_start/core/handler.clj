(ns compojure-start.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.core :refer [wrap-shiro]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(app-settings/init "r_s_app_settings.edn")
(db-util/init)
(sec-util/init)

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults (wrap-shiro app-routes) site-defaults))

