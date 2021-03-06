(ns core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [views :as views]
            [events]))

(defn ^:dev/after-load mount
  []
  (reagent/render [views/app] (.getElementById js/document "app")))

(defn ^:export init
  []
  (re-frame/dispatch-sync [:initialise-db])
  (mount))