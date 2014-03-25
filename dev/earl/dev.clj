(ns earl.dev
  (:use earl.web)
  (:require [earl.web :refer :all]
    [earl.cluster-state :as cluster-state]))

(defonce zk-client (cluster-state/connect-client "localhost:2181"))

(def handler (earl-routes zk-client))
