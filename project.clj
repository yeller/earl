(defproject earl "0.1.0-SNAPSHOT"
  :description "A simple management interface for ordasity"
  :url "https://github.com/tcrayford/earl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive  "1.1.5"]
                 [ring "1.2.0" :exclusions [ns-tracker]]
                 [compojure "1.1.5"]
                 [info.sunng/ring-jetty9-adapter "0.3.0"]
                 [org.apache.curator/curator-framework "2.4.1"]])
