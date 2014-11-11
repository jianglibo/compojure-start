(ns compojure-start.dutil.copy-file
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as shell]))

(def bower "bower_components")

(def dst (io/file "resources" "public" "js"))

(def file-to-copy '[
                     [ember ember.min.js]
                     [ember ember.js]
                     [ember-data ember-data.min.js]
                     [ember-data ember-data.js]
                     [handlebars handlebars.min.js]
                     [jquery dist jquery.min.js]
                     [jquery dist jquery.min.map]
                    ])

(defn file-pairs
  []
  (map (fn [ary]
         (vector (apply io/file bower ary) (io/file dst (last ary))))
       (map #(map name %) file-to-copy)))


(defn -main
  [& args]
  (if-not (.exists dst)
    (.mkdirs dst))
  (doseq [fp (file-pairs)]
    (io/copy (fp 0) (fp 1))))

; lein run -m compojure-start.dutil.copy-file

(def tl [:a :b :c :d :e :f])

(defn create-pairs []
  (let [mi (map-indexed list tl)
        evs (map last (filter #(even? (first %)) mi))
        ods (map last (filter #(odd? (first %)) mi))]
    (doseq [[x y] [evs ods]]
      (println x y)
      )
    )
  )

(with-out-str
(create-pairs))
