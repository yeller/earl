(ns earl.web
  (:gen-class)
  (:require clojure.java.io
            [clojure.edn :as edn]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [earl.cluster-state :as cluster-state]))

;; TODO: show unclaimed work units and alert on them

(html/defsnippet cluster-name-snippet "public/index.html"
  [:div.col-sm-4 :ul]
  [active-cluster clusters]
  [[:li html/last-of-type]] nil
  [[:li html/last-of-type]] (html/clone-for [cluster-name (sort clusters)]
                                            [:li :a]
                                            (html/do->
                                              (html/content cluster-name)
                                              (html/set-attr :href (str "/" cluster-name)))
                                            [:li] (html/set-attr :class (if (= active-cluster cluster-name) "active" ""))))

(defn sum [xs] (reduce + xs))

(defn percentage-workload [my-load nodes]
  (let [total-workload (sum (vals nodes))]
    (* 100
    (float (/ my-load total-workload)))))

(html/defsnippet node-snippet "public/index.html"
  [:div.nodes-workloads]
  [nodes]
  [:div.node-workload] (html/clone-for [[node-name my-load] nodes]
                                       [:h4 :small] (html/content node-name)
                                       [:div.progress-bar]
                                       (html/do->
                                         (html/set-attr :aria-valuenow (percentage-workload my-load nodes))
                                         (html/set-attr :style (str "width: " (percentage-workload my-load nodes) "%;")))
                                       [:div.progress-bar :span]
                                       (html/content (str (percentage-workload my-load nodes) "%"))))

(html/defsnippet work-unit-state-snippet "public/index.html"
  [:tbody.work-unit-states]
  [state config]
  [:tr.work-unit-state] (html/clone-for [[work-unit-name {:keys [load node]}] (reverse (sort-by (comp :load second) (:work-units state)))]
                                        [:td.work-unit-name] (html/content work-unit-name)
                                        [:td.work-unit-node] (html/content node)
                                        [:td.work-unit-load] (html/content (str load))))

(html/deftemplate cluster-state-page "public/index.html"
  [state config]
  [:title] (html/content (str (:earl/brand config) " Cluster Management"))
  [:a.brand-title] (html/content (str (:earl/brand config) " Cluster Management"))
  [:p.navbar-text] (html/content (:earl/quote config "Riding in the city and knocking out in the Starbucks"))
  [:span.brand-name] (html/content (:earl/brand config))
  [:span.cluster-name] (html/content (:cluster-name state))
  [:div.col-sm-4] (html/content (cluster-name-snippet (:cluster-name state) (:earl/clusters config)))
  [:div.nodes-workloads] (html/content (node-snippet (:load-distribution state)))
  [:tbody.work-unit-states] (html/content (work-unit-state-snippet state config)))

(defn earl-routes [client config]
  (handler/site
    (routes
      (GET "/" [& params]
           (try
             (reduce str (cluster-state-page (cluster-state/get-state client (first (:earl/clusters config))) config))
             (catch org.apache.zookeeper.KeeperException$NoNodeException e
               (.printStackTrace e)
               (println "cluster not found " (first (:earl/clusters config)))
               nil)))

      (GET "/:cluster-name" [& params]
           (try
             (reduce str (cluster-state-page (cluster-state/get-state client (:cluster-name params)) config))
             (catch org.apache.zookeeper.KeeperException$NoNodeException e
               (println "cluster not found " (:cluster-name params))
               (.printStackTrace e)
               nil)))
      (route/resources "/"))))

(defn read-config-file [filename]
  (with-open [r (java.io.PushbackReader.
                  (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (edn/read r))))

(defn -main [& args]
  (if (empty? args)
    (do
      (println "usage: java -jar earl.jar config-file")
      (System/exit 1)))
  (let [config (read-config-file (first args))
        client (cluster-state/connect-client (:zookeeper-cluster config "localhost:2181"))]
    (jetty/run-jetty (earl-routes client config) (:jetty-options config {:port 8080}))))
