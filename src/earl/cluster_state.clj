(ns earl.cluster-state
  (:require [clojure.set :as set])
  (:import (org.apache.zookeeper CreateMode ZooKeeper)
           (org.apache.curator framework.CuratorFramework framework.CuratorFrameworkFactory retry.BoundedExponentialBackoffRetry)))

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

(defn all-work [client work-unit-node]
  (list-node client (str "/" work-unit-node)))

(defn assigned-work [client cluster-name]
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

(defn unclaimed-work [client config cluster-name]
  (let [assigned-work (assigned-work client cluster-name)
        all-work (all-work client ((:earl/clusters config) cluster-name))]
    (set/difference
      (into #{} all-work)
      (into #{} (keys assigned-work)))))

(defn get-state
  "returns a map like this:
  {:load-distributions
   {\"node-name\" my-load}
  :cluster-name \"MyCluster\"
   :work-units
   {\"work-unit-name\" {:load 0.5 :node \"node-name\"}}
  :unclaimed-work
  #{\"work-unit-name\"}}"
  [client config cluster-name]
  {:load-distribution
   (get-load-distribution client cluster-name)
   :cluster-name
   cluster-name
   :work-units
   (assigned-work client cluster-name)
   :unclaimed-work
   (unclaimed-work client config cluster-name)})

(defn connect-client [^String zookeeper-connection-string]
  (let [client
        (CuratorFrameworkFactory/newClient zookeeper-connection-string (BoundedExponentialBackoffRetry. 5 100 10))]
    (.start client)
    client))
