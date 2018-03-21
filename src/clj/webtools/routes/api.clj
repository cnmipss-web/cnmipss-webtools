(ns webtools.routes.api
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes]]
            [ring.util.http-response :as resp]
            [webtools.config :refer [env]]
            [webtools.constants :refer [max-cookie-age]]
            [webtools.db.core :as db]
            [webtools.email :as email]
            [webtools.json :refer :all]
            [webtools.models.procurement.core :as p :refer :all]
            [webtools.util :refer :all]
            [webtools.wordpress-api :as wp]))

(def truthy (comp some? #{"true" true}))

(defn json-response
  "Pass a JSON body to supplied ring response fn"
  [ring-response body]
  (-> (edn->json body)
      ring-response
      (resp/header "Content-Type" "application/json")))

(defmacro query-route
  "Performs a db query and places the results in the response object as {:body results} in 
  JSON format with JSON headers. 

  Will perform body parameter before performing query (useful for post routes that modify the db
  before returning the results of a query).

  If there is an error, the response object becomes {:body {:error error}}
  in JSON format with JSON headers."
  ([q] `(query-route ~q nil))
  ([q & body]
   `(try
      ~@body
      (json-response resp/ok (~q))
      (catch Exception e#
        (log/error e#)
        (json-response resp/internal-server-error (.getMessage e#))))))

(defn get-all-procurement
  "Retrieve all procurements relations from the DB and return a hash-map containing those relations."
  []
  {:pnsa (db/get-all-pnsa)
   :addenda (db/get-all-addenda)
   :subscriptions (db/get-all-subscriptions)})

(defn clear-procurement
  "Delete all records associated with a PSAnnouncement id and delete WP media files associated with that id"
  [type {:keys [id] :as body}]
  (let [uuid (make-uuid id)
        query-map {:proc_id uuid}]
    (try
      ;; Delete announcement file and spec file from WP media files
      (wp/delete-media id)
      (wp/delete-media (str id "-spec"))
      (catch Exception e
        (log/error e)))
    (let [addenda (db/get-addenda query-map)
          subscriptions (p/get-subs-from-db uuid)]
      (mapv db/delete-subscription! subscriptions)
      (mapv db/delete-addendum! addenda)
      (mapv (comp wp/delete-media :id) addenda)
      (db/delete-pnsa! body))))

(def error-msg {:duplicate "Duplicate subscription.  You have already subscribed to this announcement with that email address.  "
                :other-sql "Error performing SQL transaction.  "
                :unknown "Unknown error.  Please contact webmaster@cnmipss.org for assistance. "})

(defroutes api-routes
  (GET "/api/all-certs" [] (query-route db/get-all-certs))

  (GET "/api/all-jvas" [] (query-route db/get-all-jvas))

  (GET "/api/all-procurement" [] (query-route get-all-procurement))

  (POST "/api/subscribe-procurement" {:keys [body] :as request}
        (let [{:keys [company person email tel proc_id]} body
              pid (p/make-uuid proc_id)
              existing-subs (p/get-subs-from-db pid)
              subscription {:id (java.util.UUID/randomUUID)
                            :proc_id pid
                            :company_name company
                            :contact_person person
                            :email email
                            :telephone (read-string (clojure.string/replace tel #"\D" ""))
                            :subscription_number (count existing-subs)}]
          (try
            (let [created (db/create-subscription! subscription)
                  pns (get-pns-from-db pid)]
              (future (email/confirm-subscription subscription pns))
              (future (email/notify-procurement subscription pns))
              (json-response resp/ok created))

            (catch java.sql.BatchUpdateException e
              (if-let [not-unique (->> e .getMessage (re-find #"duplicate key value violates unique constraint \"procurement_subscriptions_email_proc_id_key\""))]
                (json-response resp/internal-server-error {:message (:duplicate error-msg)})
                (json-response resp/internal-server-error {:message (str (:other-sql error-msg)
                                                                         (.getMessage e))})))

            (catch Exception e
              (json-response resp/internal-server-error {:message (str (:unknown error-msg)
                                                                       (.getMessage e))})))))
  
  (POST "/api/verify-token" request
        (if-let [token (get-in request [:cookies "wt-token" :value])]
          (let [email (get-in request [:cookies "wt-email" :value])
                user-email (keyed [email])
                correct-token ((db/get-user-token user-email) :token)
                user (-> (db/get-user-info user-email)
                         (dissoc :id))
                is-admin ((db/is-user-admin? user-email) :admin)]
            (if (= token correct-token)
              (json-response resp/ok (keyed [user is-admin]))
              (json-response resp/forbidden (keyed [user is-admin]))))
          (resp/forbidden)))
  
  (GET "/logout" request
       (-> (resp/found (str (env :server-uri) "#/login"))
           (resp/set-cookie "wt-token" "" {:max-age 1 :path "/webtools"})
           (resp/set-cookie "wt-email" "" {:max-age 1 :path "/webtools"}))))

(defroutes api-routes-with-auth
  (GET "/api/refresh-session" request
       (let [wt-token (get-in request [:cookies "wt-token" :value])
             wt-email (get-in request [:cookies "wt-email" :value])
             cookie-opts {:http-only true :max-age max-cookie-age :path "/webtools"}]
         (log/info "Refreshing session for: " wt-email)
         (-> (resp/ok)
             (resp/set-cookie "wt-token" wt-token cookie-opts)
             (resp/set-cookie "wt-email" wt-email cookie-opts))))
  
  (GET "/api/user" request
       (if-let [email (get-in request [:cookies "wt-email" :value])]
         (if-let [user (-> (db/get-user-info (keyed [email]))
                           (dissoc :id))]
           (json-response resp/ok {:user user})
           (resp/not-found))
         (resp/bad-request)))
  
  (GET "/api/all-users" [] (query-route db/get-all-users))
  
  (POST "/api/create-user" request
        (let [{:keys [email roles]} (request :body)
              admin (-> (get-in request [:body :admin]) truthy)
              id (java.util.UUID/randomUUID)
              user (keyed [email admin roles id])]
          (log/info "Created User: " user)
          (query-route db/get-all-users
                       (db/create-user! user)
                       (future (email/invite user)))))
  
  (POST "/api/update-user" request
        (let [{:keys [email roles admin]} (request :body)]
          (query-route db/get-all-users
                       (db/set-user-roles! (keyed [email roles]))
                       (db/set-user-admin! (keyed [email admin])))))
  
  (POST "/api/delete-user" request
        (let [{:keys [email]} (request :body)]
          (query-route db/get-all-users
                       (db/delete-user! (keyed [email])))))

  (POST "/api/update-cert" {:keys [body]}
        (query-route db/get-all-certs (db/update-cert! body)))

  (POST "/api/delete-cert" {:keys [body]}
        (query-route db/get-all-certs (db/delete-cert! body)))

  (POST "/api/update-jva" {:keys [body]}
        (let [jva (-> body
                      (db/make-sql-date :open_date)
                      (db/make-sql-date :close_date))]
          (query-route db/get-all-jvas (db/update-jva! jva))))

  (POST "/api/delete-jva" {:keys [body]}
        (query-route db/get-all-jvas
                     (try
                       (-> (db/jva-id body)
                           :id
                           .toString
                           (wp/delete-media))
                       (catch Exception e
                         (log/error e)))
                     (db/delete-jva! body)))

  (POST "/api/update-procurement" {:keys [body]}
        (let [new (convert-pns-from-map body)
              orig (get-pns-from-db (:id new))]
          (query-route get-all-procurement
                       (future (email/notify-subscribers :update orig new))
                       (change-in-db new))))

  (POST "/api/delete-rfp" {:keys [body]}
        (let [rfp (convert-pns-from-map body)]
          (query-route get-all-procurement
                       (future (email/notify-subscribers :delete :rfps rfp))
                       (clear-procurement :rfp rfp))))

  (POST "/api/delete-ifb" {:keys [body]}
        (let [ifb (convert-pns-from-map body)]
          (query-route get-all-procurement
                       (future (email/notify-subscribers :delete :ifbs ifb))
                       (clear-procurement :ifb ifb))))
  
  (GET "/api/fns-nap" request (query-route db/get-all-fns-nap))

  (POST "/api/delete-fns-nap" request
        (let [{record :body} request
              sub-str (fn substring [s] (subs s (count "/webtools/download/")))]
          (query-route db/get-all-fns-nap
                       (db/delete-fns-nap! record)
                       (io/delete-file (sub-str (:fns_file_link record)))
                       (io/delete-file (sub-str (:nap_file_link record)))
                       (io/delete-file (sub-str (:matched_file_link record)))))))


