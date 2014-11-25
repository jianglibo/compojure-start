(ns compojure-start.ring-shiro.t_realm
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [compojure-start.db-fixtures :as db-fixtures]
            [compojure-start.ring-shiro.sec-util :as sec-util]
            [compojure-start.ring-shiro.sec-db :as sec-db]
            [compojure-start.cljcommon
             [clj-util :as clj-util]
             [app-settings :as app-settings]
             [db-util :as db-util]] :reload-all)
  (:import (java.util Date)))


(facts "about encrypt and decrypt"
       (fact :encrypt "encrypt and decrypt same string."
             (let [raw-str "abc"
                   encrypted (sec-util/encrypt-str raw-str)
                   decrypted (sec-util/descrypt-str encrypted)]
               raw-str => decrypted))
       (fact :encrypt "encrypted length"
             (let [encrypted (sec-util/encrypt-str "abc")]
               (count encrypted) => 44)))



