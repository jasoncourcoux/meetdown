(ns meetdown.view
  (:require [petrol.core :refer [send! send-value!]]
            [meetdown.messages :as m]
            [meetdown.login.view :as lv]
            [meetdown.routes :refer [href-for]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [put!]]
            [meetdown.login.messages :as ml]))

(defn- home-view
  [ui-channel]
  [:div.col-xs-12
   (for [[link title] [[(href-for :new-event) "Create new meetdown"]]]
     [:div.row.col-xs-12 {:key title}
      [:a {:href link} title]])])

(defn- server-view
  [ui-channel server-state view]
  [:div
   [:div.row
    [:h3.col-xs-12 "Event"]]
   (for [event-attr server-state]
     (let [attr-name (-> event-attr first name)]
       [:div.row {:key attr-name}
        [:label.col-md-3 (str attr-name ":")]
        [:div.col-md-9 (second event-attr)]]))])

(defn- event-form
  [ui-channel {:keys [name speaker description] :as event}]
  [:div.table
   [:div.row.col-xs-12
    [:label.col-xs-4 "Event name:"]
    [:input.col-xs-6 {:type :text
                      :placeholder "Event name..."
                      :defaultValue name
                      :on-change (send-value! ui-channel #(m/map->ChangeEvent {:name % :speaker speaker :description description}))}]]
   [:div.row.col-xs-12
    [:label.col-xs-4 "Speaker:"]
    [:input.col-xs-6 {:type :text
                      :placeholder "Speaker..."
                      :defaultValue speaker
                      :on-change (send-value! ui-channel #(m/map->ChangeEvent {:name name :speaker % :description description}))}]]
   [:div.row.col-xs-12
    [:label.col-xs-4 "Description:"]
    [:textarea.col-xs-6 {:rows 2
                         :placeholder "Description..."
                         :defaultValue description
                         :on-change (send-value! ui-channel #(m/map->ChangeEvent {:name name :speaker speaker :description %}))}]]
   [:div.row.col-xs-12
    [:div.col-xs-4
     [:button.btn.btn-success
      {:on-click (send! ui-channel (m/->CreateEvent event))} "Create Event"]]]])


(defn event-lookup
  [ui-channel view]
  (when-let [id (get-in view [:route-params :id])]
    (put! ui-channel (m/->FindEvent id))))

(defn- header-view
  [ui-channel app]
  (prn app)
  [:div.row
   [:div.col-xs-6.col-xs-offset-3
    [:h2 "Event Management Client"]]
   [:div.col-xs-3
    (if (contains? app :authorization)
      [:a {:href (href-for :logout)} "Logout"]
      [:a {:href (href-for :login)} "Login"])]])

(defn root
  [ui-channel {:keys [event server-state login view] :as app}]
  [:div
   [:div.container
    (header-view ui-channel app)
    [:div.row.col-xs-12 [:br]]
    (match [(:handler view)]
           [:login]       [lv/login-view ui-channel login]
           [:logout]      (put! ui-channel (ml/->Logout))
           [:new-event]   [event-form ui-channel event]
           [:event]       (event-lookup ui-channel view)
           [:event-found] [server-view ui-channel server-state view]
           :else          [home-view ui-channel])]])
