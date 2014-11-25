(ns compojure-start.cljcommon.app-settings-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [compojure-start.cljcommon
             [app-settings :as app-settings]] :reload-all)
  (:import (java.util Date)))


(deftest b-t
  (binding [app-settings/bingding-test (ref 5)]
    (is (= 5 (app-settings/get-binding-test))))
  (is (= nil (app-settings/get-binding-test))))

;(deftest require-test
;  (let [dbschema (app-settings/get-setting :db-schema)]
;    (require `[~(symbol dbschema)])
;    (alias 'db-schema (symbol dbschema))
;    (is (find-ns 'db-schema))))

