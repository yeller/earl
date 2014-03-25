(ns earl.cluster-state
  (:import (org.apache.zookeeper CreateMode ZooKeeper)
           (org.apache.curator framework.CuratorFramework framework.CuratorFrameworkFactory retry.BoundedExponentialBackoffRetry)
           ))

;; TODO
;; eventually display how handoff is going?

(defn get-node [^CuratorFramework client ^String k]
  (-> client .getData (.forPath k) String.))

(defn list-node [^CuratorFramework client ^String k]
  (map #(String. %)
       (-> client .getChildren (.forPath k))))

(defn get-load-distribution [client cluster-name]
  (->> (list-node client (str "/" cluster-name "/claimed-work"))
    (map
      (fn [work-unit]
        [work-unit
         (get-node client (str "/" cluster-name "/claimed-work/" work-unit))
         (get-node client (str "/" cluster-name "/meta/workload/" work-unit))]))
    (group-by second)
    (map
      (fn [[node vs]]
        [node (reduce + (map #(Float/parseFloat %) (map last vs)))]))
    (into {})))

(defn get-work-units [client cluster-name]
  (into {}
        (map
          (fn [work-unit]
            [work-unit
             {:load
              (Float/parseFloat
                (get-node client (str "/" cluster-name "/meta/workload/" work-unit)))
              :node
              (get-node client (str "/" cluster-name "/claimed-work/" work-unit))}])
          (list-node client (str "/" cluster-name "/meta/workload")))))

(defn get-state
  "returns a map like this:
  {:load-distributions
   {\"node-name\" my-load}
   :work-units
   {\"work-unit-name\" {:load 0.5 :node \"node-name\"}}}"
  [client cluster-name]
  {:load-distribution
   (get-load-distribution client cluster-name)
   :work-units
   (get-work-units client cluster-name)})

(defn connect-client [^String zookeeper-connection-string]
  (let [client
        (CuratorFrameworkFactory/newClient zookeeper-connection-string (BoundedExponentialBackoffRetry. 5 100 10))]
    (.start client)
    client))
