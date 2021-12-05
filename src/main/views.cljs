(ns views
  (:require [re-frame.core :as re-frame]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [reagent.core :as reagent]))

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
           [:span.ion-close-round]]]
         [:div.modal-body
          [:form
           [input-form-group "Title" (reagent/cursor model [:title])]
           [input-form-group "Description" (reagent/cursor model [:description])]
           [input-form-group "Building" (reagent/cursor model [:building])]
           ; category
           [select-form-group "Category"
            ["cleaning" "security" "electricity" "temperature"]
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

(defn nav-bar
  []
  [:div.navbar.box-shadow.bg-white
   [:div.container.d-flex.justify-content-between
    [:a.navbar-brand.d-flex.align-items-center "Issues"]
    [:button.navbar-toggler
     {:on-click #(re-frame/dispatch [:show-modal true])}
     [:span.ion-plus-round]]]])

(defn dashboard-card [{:keys [id title building category status]}]
  [:div.card.mb-4.shadow-sm.rounded.bg-white
   {:draggable   true
    :onDragStart #(.setData (.-dataTransfer %) "id" id)
    :data-key    id}
   [:div.card-body
    [:h5.card-title title]
    [:div.text-secondary building]
    [:div.text-secondary "Category: " category]
    [:div.text-secondary "Issue id: #" id ", status: " status]]])

(defn dashboard-column [title status cards]
  [:div.col-md-4
   {:onDrop     (fn [e]
                  (.preventDefault e)
                  (when-let [id (.getData (.-dataTransfer e) "id")]
                    (re-frame/dispatch [:change-issue-status id status])))
    :onDragOver #(.preventDefault %)}
   [:h2.sticky-top.bg-white title [:span.badge.badge-light (count cards)]]
   (for [{:keys [id] :as card} cards]
     ^{:key id} [dashboard-card card])])

(defn app
  []
  (let [show-modal? (re-frame/subscribe [:show-modal])]
    [:<>
     (when @show-modal? [modal])
     [nav-bar]
     [:div.container-fluid
      (let [{:keys [in-progress done todo]} @(re-frame/subscribe [:issues])]
        [:div.row
         [dashboard-column "Todo" :todo todo]
         [dashboard-column "In progress" :in-progress in-progress]
         [dashboard-column "Done" :done done]])]]))