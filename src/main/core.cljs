(ns core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [views :as views]
            [events]))

(defn ^:export init
  []
  (re-frame/dispatch-sync [:initialise-db])
  (reagent/render [views/app] (.getElementById js/document "app")))