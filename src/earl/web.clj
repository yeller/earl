(ns earl.web
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [earl.cluster-state :as cluster-state]))

(html/deftemplate cluster-state-page "index.html"
  [state config]
  [:title] (html/content "lol"))

(def config
  {:earl/brand "Yeller"
   :earl/clusters
   #{"YellerDev"}})

(defn earl-routes [client]
  (routes
    (route/resources "/")
    (GET "/" [& params]
         (reduce str (cluster-state-page (cluster-state/get-state client (first (:earl/clusters config))) config)))

    (GET "/:cluster-name" [& params]
         (reduce str (cluster-state-page (cluster-state/get-state (:cluster-name params)) config)))))

(defonce zk-client (cluster-state/connect-client "localhost:2181"))

(def handler (handler/site (earl-routes zk-client)))
