(ns webtools.util
  (:require [webtools.constants :as const]
            [clojure.spec.alpha :as s]
            [clojure.string :as cstr]
            #?(:clj  [clj-time.core :as time]
               :cljs [cljs-time.core :as time])
            #?(:clj  [clj-time.coerce :as coerce]
               :cljs [cljs-time.coerce :as coerce])
            #?(:clj  [clj-time.format :as format]
               :cljs [cljs-time.format :as format])))

(defmacro get-version []
  (let [git-version (clojure.edn/read-string (slurp "resources/version.edn"))]
    (:version git-version)))

(defmacro pull
  "Pull all symbols from ns into the calling ns.  Used to refer-through, providing 
  a wrapper for the target ns."
  {:style/indent 1}
  [ns vlist]
  `(do ~@(for [i vlist]
           `(def ^{:doc "Test"} ~i ~(symbol (str ns "/" i))))))

(let [transforms {:keys keyword 
                  :strs str
                  :syms identity}]
  (defmacro keyed
    "Create a map in which, for each symbol S in vars, (keyword S) is a
  key mapping to the value of S in the current scope. If passed an optional
  :strs or :syms first argument, use strings or symbols as the keys instead."
    ([vars] `(keyed :keys ~vars))
    ([key-type vars]
     (let [transform (comp (partial list `quote)
                           (transforms key-type))]
       (into {} (map (juxt transform identity) vars))))))

(defn full-response-format [body-format]
  (-> (body-format)
      (update :read (fn [original-handler]
                      (fn [response-obj]
                        {:headers  #?(:clj (:headers response-obj)
                                      :cljs (js->clj (.getResponseHeaders response-obj)))
                         :body    (original-handler response-obj)
                         :status  #?(:clj (:status response-obj)
                                     :cljs (.getStatus response-obj))})))))

(defn format-tel-num
  [tel]
  (let [suffix (-> tel (mod 10000))
        rm-suffix #(-> % (- suffix) (/ 10000))
        prefix (-> tel (rm-suffix) (mod 1000))
        rm-prefix #(-> % (- prefix) (/ 1000))
        area-code (-> tel rm-suffix rm-prefix (mod 1000))
        rm-ac #(-> % (- area-code) (/ 1000))
        country-code (-> tel rm-suffix rm-prefix rm-ac)]
    (str
     (if (> country-code 0)
       (str "+" country-code " "))
     (if (> area-code 0)
       (str "(" area-code ") "))
     prefix "-" suffix)))

(defn select-non-nil
  [a b]
  (or a b))

(defn line-parser
  [re line]
  (if-let [match (peek (re-find re line))]
    (cstr/trim match)
    nil))

#?(:clj
   (do (defn make-status
         [record]
         (let [{:keys [close_date]} record
               today (time/now)
               end (if (instance? java.sql.Date close_date)
                     (coerce/from-sql-date close_date)
                     close_date)]
           (if (nil? end)
             (assoc record :status true)
             (if (time/before? today end)
               (assoc record :status true)
               (assoc record :status false)))))
       (s/fdef make-status
               :args (s/cat :record (s/alt :procurement :webtools.spec.procurement/record
                                           :jva map?))
               :ret (s/or :procurement :webtools.spec.procurement/record
                          :jva map?))))


(defmacro try-let
  [bindings & body]
  (assert (even? (count bindings))
          "try-let needs an even number of forms in binding vector")
  (let [bindings-ls (take-nth 2 bindings)
        gensyms (take (count bindings-ls) (repeatedly gensym))
        [thens stanzas] (split-with #(not (and (list? %) (= (first %) 'catch))) body)]
    `(let [[ok# ~@gensyms]
           (try
             (let [~@bindings] [true ~@bindings-ls])
             ~@(map
                (fn [stanza]
                  (assert (>= (count stanza) 3)
                          "Malformed stanza")
                  (let [[x y z & body] stanza]
                    (assert (= x 'catch)
                            "Only catch stanzas are allowed")
                    `(~x ~y ~z [false (do ~@body)])))
                stanzas))]
       (if ok#
         (let [~@(interleave bindings-ls gensyms)]
           ~@thens)
         ~(first gensyms)))))

(defn capitalize-words 
  "Capitalize every word in a string"
  [s]
  (->> (cstr/split (str s) #"\b") 
       (map cstr/capitalize)
       cstr/join))

(defn cookie->map
  [cookie-string]
  (when cookie-string
    (into {}
          (for [cookie (.split cookie-string ";")]
            (let [keyval (map #(.trim %) (.split cookie "=" 2))]
              [(first keyval) (second keyval)])))))

(defn email->name [email]
  (as-> (cstr/split email #"@") names
    (first names)
    (cstr/split names #"\.")
    (map cstr/capitalize names)
    (cstr/join " " names)))
