(defproject cycletics "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [org.clojure/core.async "0.2.374"]
                 [clj-time "0.11.0"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot cycletics.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
