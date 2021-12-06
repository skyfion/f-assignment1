(ns views
  (:require [re-frame.core :as re-frame]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [reagent.core :as reagent]))

(def categories ["cleaning" "security" "electricity" "temperature"])

(defn input-form-group
  [title state]
  [:div.form-group
   [:label title]
   [:input.form-control
    {:value     (or @state "")
     :on-change #(reset! state (-> % .-target .-value))}]])

(defn select-option
  [value]
  [:option {:value value} (str/capitalize value)])

(defn select-form-group
  [title items state]
  (let [comp (atom nil)]
    (fn [title items state]
      [:div.form-group
       [:label title]
       (into
         [:select.custom-select
          {:ref       #(reset! comp %)
           :on-change #(when-let [index (some-> @comp (.-selectedIndex))]
                         (reset! state (nth items index)))}]
         (for [item items]
           [select-option item]))])))

(defn modal
  []
  (let [close-fn #(re-frame/dispatch [:show-modal false])
        model (reagent/atom {:category "cleaning"
                             :status   :todo})]
    (fn []
      [:div.modal
       {:tabIndex -1 :role :dialog :style {:display :block}}
       [:div.modal-dialog
        [:div.modal-content
         [:div.modal-header
          [:h5.modal-title "Add issue"]
          [:button.close {:on-click close-fn}
           [:span "×"]]]
         [:div.modal-body
          [:form
           [input-form-group "Title" (reagent/cursor model [:title])]
           [input-form-group "Description" (reagent/cursor model [:description])]
           [input-form-group "Building" (reagent/cursor model [:building])]
           ; category
           [select-form-group "Category" categories
            (reagent/cursor model [:category])]]]
         [:div.modal-footer
          [:button.btn.btn-secondary
           {:on-click close-fn}
           "Close"]
          [:button.btn.btn-primary
           {:on-submit #(.preventDefault %)
            :on-click  (fn []
                         (re-frame/dispatch [:add-issue @model])
                         (close-fn))}
           "Add"]]]]])))

(defn btn-category
  [title active?]
  [:button.btn.btn-primary
   {:class    [(when active? :active)]
    :on-click #(re-frame/dispatch [:category-filter title])}
   title])

(defn filter-category
  []
  (let [filter-category (re-frame/subscribe [:category-filter])]
    [:div.btn-group.btn-group-sm.mr-3
     (doall
       (for [c categories]
         ^{:key (hash c)}
         [btn-category c (get @filter-category c)]))]))

(defn search-bar
  []
  (let [state (reagent/atom "")
        search-fn #(re-frame/dispatch [:search-term @state])]
    (fn []
      [:<>
       [:input.form-control.mr-sm-2
        {:type      :search :placeholder "Search"
         :value     (or @state "")
         :on-change #(let [v (-> % .-target .-value)]
                       (when (str/blank? v)
                         (re-frame/dispatch [:search-term ""]))
                       (reset! state v))}]
       [:button.btn.btn-outline-success.my-2.my-sm-0
        {:on-click search-fn}
        "Search"]])))

(defn nav-bar
  []
  [:header.navbar.box-shadow.navbar-expand-lg.navbar-light.bg-light
   [:div.d-flex.justify-content-between.w-100
    [:a.navbar-brand.d-flex.align-items-center "Issues"]
    [:form.form-inline.my-2.my-lg-0.py-2.ml-auto
     [:button.btn.btn-light.mr-3
      {:on-click #(re-frame/dispatch [:show-modal true])}
      "Add issue"]
     [filter-category]
     [search-bar]]]])

(defn dashboard-card [{:keys [id title building category status]}]
  [:div.card.mb-4.shadow-sm.rounded.bg-white
   {:style       {:cursor :grab}
    :draggable   true
    :onDragStart #(.setData (.-dataTransfer %) "id" id)
    :data-key    id}
   [:div.card-body
    [:h5.card-title title]
    [:div.text-secondary building]
    [:div.text-secondary "Category: " category]
    [:div.text-secondary "Issue id: #" id ", status: " status]]])

(defn dashboard-column
  [title status cards]
  [:div.col-md-4
   {:onDrop     (fn [e]
                  (.preventDefault e)
                  (when-let [id (.getData (.-dataTransfer e) "id")]
                    (re-frame/dispatch [:change-issue-status id status])))
    :onDragOver #(.preventDefault %)}
   [:h2.sticky-top.bg-white title [:span.badge.badge-light (count cards)]]
   (doall
     (for [{:keys [id] :as card} cards]
       ^{:key id} [dashboard-card card]))])

(defn app
  []
  (let [show-modal? (re-frame/subscribe [:show-modal])]
    [:<>
     (when @show-modal? [modal])
     [nav-bar]
     [:div.container-fluid
      (let [{:keys [in-progress done todo]} @(re-frame/subscribe [:issues-model])]
        [:div.row
         [dashboard-column "Todo" :todo todo]
         [dashboard-column "In progress" :in-progress in-progress]
         [dashboard-column "Done" :done done]])]]))