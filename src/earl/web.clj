(ns earl.web
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [earl.cluster-state :as cluster-state]))

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
  [:tr.work-unit-state] (html/clone-for [[work-unit-name {:keys [load node]}] (sort-by (comp :load second) (:work-units state))]
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
      (route/resources "/")
      (GET "/" [& params]
           (reduce str (cluster-state-page (cluster-state/get-state client (first (:earl/clusters config))) config)))

      (GET "/:cluster-name" [& params]
           (try
             (reduce str (cluster-state-page (cluster-state/get-state client (:cluster-name params)) config))
             (catch org.apache.zookeeper.KeeperException$NoNodeException e nil))))))

