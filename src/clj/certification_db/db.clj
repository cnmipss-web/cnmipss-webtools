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
                                     [effect-date :string]
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
(def ^:private email->user '[:find ?e ?email ?type ?token :in $ ?email
                             :where [?e :user/email ?email]
                             [?e :user/type ?type]
                             [?e :user/token ?token]])

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

(defn user-type-q?
  [email]
  )
