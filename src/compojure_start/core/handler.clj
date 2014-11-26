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


(defresource parameter [txt]
  :available-media-types ["text/plain"]
  :handle-ok (fn [_] (format "The text is %s" txt)))


(def posts (ref []))

(defresource postpost []
  :allowed-methods [:post :get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (format  (str "<html>Post text/plain to this resource.<br>\n"
                             "There are %d posts at the moment.")
                        (count @posts)))
  :post! (fn [ctx]
           (dosync
            (let [body (slurp (get-in ctx [:request :body]))
                  id   (count (alter posts conj body))]
              {::id id})))
  ;; actually http requires absolute urls for redirect but let's
  ;; keep things simple.
  :post-redirect? (fn [ctx] {:location (format "/postbox/%s" (::id ctx))}))


(defroutes app-routes
  (GET "/" [] "Hello World")
  (ANY "/rest/:txt" [txt] (parameter txt))
  (ANY "/postbox" [] (postpost))
;  (GET "/" [] (io/resource "public/index.html"))
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
