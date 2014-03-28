(ns earl.web
  (:gen-class)
  (:require clojure.java.io
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [earl.cluster-state :as cluster-state]))

;; TODO: show unclaimed work units and alert on them

(html/defsnippet cluster-name-snippet "public/index.html"
  [:div.left-nav :ul]
  [active-cluster clusters]
  [[:li html/last-of-type]] nil
  [[:li html/last-of-type]] (html/clone-for [cluster-name (sort clusters)]
                                            [:li :a]
                                            (html/do->
                                              (html/content cluster-name)
                                              (html/set-attr :href (str "/cluster/" cluster-name)))
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
  [state]
  [:tr.work-unit-state] (html/clone-for [[work-unit-name {:keys [load node]}] (reverse (sort-by (comp :load second) (:work-units state)))]
                                        [:td.work-unit-name] (html/content work-unit-name)
                                        [:td.work-unit-node] (html/content node)
                                        [:td.work-unit-load] (html/content (str load))))

(html/defsnippet unassigned-work-unit-snippet "public/index.html"
  [:div.panel.unassigned-work-units :div.panel-body]
  [state]
  [:tbody.unassigned-work-units :tr.unassigned-work-unit]
  (if (empty? (:unclaimed-work state))
    nil
    (html/clone-for [work-unit-name (sort (:unclaimed-work state))]
                    [:td.work-unit-name] (html/content work-unit-name)))
  [:p.unclaimed-work-unit-description]
  (if (empty? (:unclaimed-work state))
    (html/content "There are currently no unclaimed work units")
    (html/content "These are all the work units that aren't being worked on. Likely they'll be claimed soon")))

(html/deftemplate cluster-state-page "public/index.html"
  [state config]
  [:title] (html/content (str (:earl/brand config) " Cluster Management"))
  [:a.brand-title] (html/content (str (:earl/brand config) " Cluster Management"))
  [:p.navbar-text] (html/content (:earl/quote config "Riding in the city and knocking out in the Starbucks"))
  [:span.brand-name] (html/content (:earl/brand config))
  [:span.cluster-name] (html/content (:cluster-name state))
  [:div.left-nav] (html/content (cluster-name-snippet (:cluster-name state) (keys (:earl/clusters config))))
  [:div.nodes-workloads] (html/content (node-snippet (:load-distribution state)))
  [:tbody.work-unit-states] (html/content (work-unit-state-snippet state))
  [:div.panel.unassigned-work-units :div.panel-body] (html/content (unassigned-work-unit-snippet state)))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn show-cluster [client config cluster-name]
  (try
    (reduce str (cluster-state-page (cluster-state/get-state client config cluster-name) config))
    (catch org.apache.zookeeper.KeeperException$NoNodeException e
      (println "cluster not found " cluster-name)
      (.printStackTrace e)
      nil)))

(defn earl-routes [client config]
  (handler/site
    (routes
      (GET "/" req
           (show-cluster client config (first (keys (:earl/clusters config)))))

      (GET "/cluster/:cluster-name" req
           (let [cluster-name (:cluster-name (:params req))]
             (if (json-request? req)
               {:body (json/write-str (cluster-state/get-state client config cluster-name ))
                :status 200}
               (show-cluster client config cluster-name))))
      (route/resources "/"))))

(defn read-config-file [filename]
  (with-open [r (java.io.PushbackReader.
                  (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (edn/read r))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
      (catch Exception e
        (.printStackTrace e)
        {:status 500
         :body "<body>HTTP 500. Look at the logs</body>"}))))

(defn -main [& args]
  (if (empty? args)
    (do
      (println "usage: java -jar earl.jar config-file")
      (System/exit 1)))
  (let [config (read-config-file (first args))
        client (cluster-state/connect-client (:zookeeper-cluster config "localhost:2181"))]
    (jetty/run-jetty (wrap-error-page (earl-routes client config)) (:jetty-options config {:port 8080}))))
