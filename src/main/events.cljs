(ns events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs.reader :as edn]))

(re-frame/reg-sub
  :data
  (fn [db _]
    (-> db :data)))

(re-frame/reg-event-fx
  :initialise-db
  (fn []
    {:http-xhrio {:method          :get
                  :uri             "/data.edn"
                  :timeout         8000
                  :response-format (ajax/text-response-format)
                  :on-success      [:on-data-success]
                  :on-failure      [:on-data-error]}}))

(re-frame/reg-event-db
  :on-data-success
  (fn [db [_ result]]
    (assoc db :data (edn/read-string result))))

(re-frame/reg-event-fx
  :on-data-error
  (fn [_ [_ result]]                                        ; todo
    (.log js/console "error" result)))