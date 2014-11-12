(ns compojure-start.ring-shiro.const)

(def PASSWORD_HASH_ITERATIONS 1024)


(def USER_REALM_NAME "jdbc-realm")

(def db-unique-keys {:user #{:id :email :username :mobile}
                     :group4u #{:id :name}
                     :role #{:id :name}})
