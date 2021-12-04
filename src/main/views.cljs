(ns views
  (:require [re-frame.core :as re-frame]))


(defn app
  []
  (let [data @(re-frame/subscribe [:data])]
    [:div
     [:h2 "todo"]
     (for [{:keys [title]} data]
       [:div title])]))