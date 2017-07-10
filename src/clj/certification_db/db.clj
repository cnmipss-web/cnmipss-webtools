(ns certification-db.db
  (:require [datomic.api :as d]
            [datomic-schema.schema :as s]
            [mount.core :as mount]))

(mount/defstate ^{:on-reload :noop}
  conn
  :start
  (d/connect "datomic:dev://localhost:4334/hello")
  :stop
  (d/shutdown false))

(defn- reset-db!
  [db-uri]
  (if (d/delete-database db-uri)
    (d/create-database db-uri)
    false))

(def ^:private db-schema [(s/schema user
                                    (s/fields
                                     [email :string :indexed :unique-value]
                                     [type :enum [:user :admin]]
                                     [name :string]
                                     [token :string :unique-value :indexed]))
                          (s/schema certification
                                    (s/fields
                                     [last-name :string]
                                     [first-name :string]
                                     [mi :string]
                                     [type :string :indexed]
                                     [cert-no :string :indexed :unique-value]
                                     [start-date :string]
                                     [expiry-date :string]))])

(defn- init-schema
  []
  (d/transact conn (s/generate-schema db-schema)))


(defn- create-user!
  [user]
  (d/transact conn [user]))

(def ^:private email->id '[:find ?e :in $ ?email :where [?e :user/email ?email]])
(def ^:private email->type '[:find ?type :in $ ?email :where [?e :user/email ?email] [?e :user/type ?type]])
(def ^:private email->token '[:find ?token :in $ ?email :where [?e :user/email ?email] [?e :user/token ?token]])
(def ^:private email->user '[:find ?e ?email ?name ?type :in $ ?email
                             :where [?e :user/email ?email]
                             [?e :user/name ?name]
                             [?e :user/type ?type]])
(def ^:private cert-no->cert '[:find ?ln ?fn ?mi ?type ?cert-no ?s-d ?e-d :in $ ?cert-no
                               :where
                               [?e :certification/cert-no ?cert-no]
                               [?e :certification/last-name ?ln]
                               [?e :certification/first-name ?fn]
                               [?e :certification/mi ?mi]
                               [?e :certification/type ?type]
                               [?e :certification/start-date ?s-d]
                               [?e :certification/expiry-date ?e-d]])
(def ^:private all-certs '[:find ?ln ?fn ?mi ?type ?cert-no ?s-d ?e-d
                               :where
                               [?e :certification/cert-no ?cert-no]
                               [?e :certification/last-name ?ln]
                               [?e :certification/first-name ?fn]
                               [?e :certification/mi ?mi]
                               [?e :certification/type ?type]
                               [?e :certification/start-date ?s-d]
                               [?e :certification/expiry-date ?e-d]])

(def ^:private match-admin '[:find ?e :in $ ?email
                             :where
                             [?e :user/type :user.type/admin]
                             [?e :user/email ?email]])

(defn set-user-token
  [email token]
  (d/transact conn [{:db/id [:user/email email]
                     :user/token token}]))

(defn get-user-token
  [email]
  (first (first (d/q email->token (d/db conn) email))))

(defn get-user-info
  [email]
  (first (d/q email->user (d/db conn) email)))

(defn user-admin?
  [email]
  (d/q match-admin (d/db conn) email))

(defn post-cert
  [certification]
  (d/transact conn [certification]))

(defn update-cert
  [certification]
  (first (d/transact conn [certification])))

(defn get-cert [num]
  (d/q cert-no->cert (d/db conn) num))

(defn get-all-certs []
  (d/q all-certs (d/db conn)))
