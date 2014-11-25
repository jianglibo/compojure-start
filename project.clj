(defproject compojure-start "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :java-source-paths ["src/main/java"]
  :javac-options ["-encoding" "UTF-8"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.2.0"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.apache.shiro/shiro-core "1.2.3"]
                 [org.apache.shiro/shiro-ehcache "1.2.3"]
                 [com.google.guava/guava "18.0"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [org.clojure/tools.logging "0.3.1"]
                 [environ "1.0.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.hsqldb/hsqldb "2.3.2"]
                 [ring/ring-defaults "0.1.2"]]
  :target-path "target/%s"
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler compojure-start.core.handler/app}
  :jvm-opts ^:replace ["-Dnet.sf.ehcache.skipUpdateCheck=true"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]
                                  [midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.1"]]}})
