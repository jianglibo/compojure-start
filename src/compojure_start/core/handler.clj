(ns compojure-start.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [liberator.core :refer [resource defresource]]
            [clojure.java.io :as io]
            [compojure-start.cljcommon
             [db-util :as db-util]
             [app-settings :as app-settings]]
            [compojure-start.ring-shiro.core :refer [wrap-shiro]]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(defroutes app-routes
  (GET "/" [] "Hello World")
  (ANY "/rest/user" [] (user-res/users))
  (ANY "/rest/user/:id" [id] (user-res/user id))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults (wrap-shiro app-routes)
                 (-> (dissoc site-defaults :session)
                     (assoc :static {:resources "public"}))))


;wrap-defaults total has 18 wraps,
;site-defaults is an hash config, which default include: params,cookies,session,security,static,responses
;defroutes return an handler.

;compujure app start.
;lein ring server-headless actualy find handler function in project.clj :ring {:handler compojure-start.core.handler/app}

;ring.adapter.jetty expose
;^Server run-jetty [handler options]
