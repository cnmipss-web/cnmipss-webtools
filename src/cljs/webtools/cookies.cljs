(ns webtools.cookies
  (:import goog.net.Cookies))

(def cookies (Cookies. js/document))

(defn set-cookie [k v & opts]
  "Sets a cookie.
   Options:
   max-age -- The max age in seconds (from now). Use -1 to set a session cookie. If not provided, the default is -1 (i.e. set a session cookie).
   "
  (when-let [k (and (.isValidName cookies (name k)) (name k))]
    (when (.isValidValue cookies v)
      (let [{:keys [max-age path domain secure?]} (apply hash-map opts)]
        (.set cookies k v max-age path domain secure?)))))

(defn get-cookie [k]
  "Returns the value for the first cookie with the given key."
  (.get cookies (name k) nil))

(defn remove-cookie [key]
  "Removes and expires a cookie."
  (.remove cookies (name key)))
