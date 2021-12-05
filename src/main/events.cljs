(ns events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs.reader :as edn]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

(re-frame/reg-sub
  :issues
  (fn [db _]
    (->> db
         :issues
         (vals)
         (group-by :status))))

(re-frame/reg-event-db
  :change-issue-status
  (fn [db [_ id status]]
    (assoc-in db [:issues id :status] status)))

(re-frame/reg-event-db
  :add-issue
  (fn [db [_ issue]]
    (let [id (str (gensym "new"))]
      (assoc-in db [:issues id] (assoc issue :id id)))))

(re-frame/reg-event-db
  :show-modal
  (fn [db [_ show?]]
    (assoc db :show-modal? show?)))

(re-frame/reg-sub
  :show-modal
  (fn [db _]
    (-> db :show-modal?)))

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
    (->> result
         (edn/read-string)
         (map #(update % :status keyword))
         (map #(update % :title str/capitalize))
         (map #(update % :id str))
         (reduce (fn [acc data] (assoc acc (:id data) data)) {})
         (assoc db :issues))))

(re-frame/reg-event-fx
  :on-data-error
  (fn [_ [_ result]]                                        ; todo
    (.log js/console "error" result)))