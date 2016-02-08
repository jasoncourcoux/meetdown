(ns meetdown.http
  (:require [org.httpkit.server :as http]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST DELETE ANY context]]
            [compojure.route :refer [files]]
            [meetdown.data :as data]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.format :refer [wrap-restful-format]]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn- handle-query
  [db-conn]
  (fn [{req-body :body-params}]
    (info "Handling query " req-body)
    {:body (case (:type req-body)
             :get-events   (data/get-events db-conn)
             :create-event (data/create-entity db-conn (:txn-data req-body)))
             :create-user  (data/create-entity db-conn (:txn-data req-body))}))

(defn make-router [db-conn]
  (-> (routes
       (files "/")
       (POST "/q" []
             (handle-query db-conn)))))

(def default-config
  {:params {:urlencoded true
            :keywordize true
            :nested     true
            :multipart  true}

   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true}})

(defn wrap-log-request
  ([handler]
    (fn [req]
      (debug "Handling request" req)
      (handler req))))

(defn make-handler [db-conn]
  (-> (make-router db-conn)
      (wrap-log-request)
      (wrap-restful-format :formats [:edn :transit-json])
      (rmd/wrap-defaults (-> rmd/site-defaults
                             (assoc-in [:security :anti-forgery] false)))))

(defrecord Server-component [server-options db-component]
  component/Lifecycle
  (start [component]
    (println "Starting http-kit")
    (let [server (http/run-server (make-handler (:connection db-component)) server-options)]
      (assoc component :web-server server)))
  (stop [component]
    (println "Shutting down http-kit")
    (let [server (:web-server component)]
      (server :timeout 100))))

(defn new-server [server-options]
  (map->Server-component {:server-options server-options}))

(comment

  (data/create-entity (user/system :db-conn) (:txn-data {:event/name "New event-2"}))

  (data/create-entity (user/system :db-conn) (:txn-data req-body))

  )
