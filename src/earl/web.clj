(ns earl.web
  (:require [compjure.core :refer :all]
            [earl.cluster-state :as cluster-state]))

(defn earl-routes [client]
  (routes
    (GET "/"
         "web app goes here")))
