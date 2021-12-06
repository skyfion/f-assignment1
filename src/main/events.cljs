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
    (-> db :issues)))

(re-frame/reg-sub
  :issues-model
  :<- [:category-filter]
  :<- [:issues]
  :<- [:search-term]
  (fn [[categories issues search-term] _]
    (->> issues
         (vals)
         (filter (fn [v]
                   (or (every? false? (vals categories))
                       (get categories (:category v)))))
         (filter (fn [v]
                   (or (str/blank? search-term)
                       (str/includes? (str/lower-case (:title v))
                                      (str/lower-case search-term)))))
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

(re-frame/reg-event-db
  :category-filter
  (fn [db [_ category]]
    (update-in db [:category-filter category] not)))

(re-frame/reg-sub
  :category-filter
  (fn [db _]
    (-> db :category-filter)))

(re-frame/reg-sub
  :search-term
  (fn [db _]
    (-> db :search-term)))

(re-frame/reg-event-db
  :search-term
  (fn [db [_ term]]
    (assoc db :search-term term)))

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