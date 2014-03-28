(defproject earl "0.1.0-SNAPSHOT"
  :description "A simple management interface for ordasity"
  :url "https://github.com/tcrayford/earl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main earl.web
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.5"]
                 [ring "1.2.0"]
                 [org.clojure/data.json  "0.2.4"]
                 [compojure "1.1.5"]
                 [org.apache.curator/curator-framework "2.4.1"]]
  :ring {:handler earl.dev/handler}
  :profiles {:dev {:plugins [[lein-ring  "0.8.10"]]
                   :source-paths ["src" "dev"]}})
