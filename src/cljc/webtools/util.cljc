(ns webtools.util
  #?(:clj  (:require [clj-time.core :as time]
                     [clj-time.coerce :as coerce]
                     [clj-time.format :as format])
     :cljs (:require [cljs-time.core :as time]
                     [cljs-time.coerce :as coerce]
                     [cljs-time.format :as format])))

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
    (clojure.string/trim match)
    nil))

(defn make-status
  [record]
  (let [{:keys [close_date]} record
        today (time/now)
        end (coerce/from-date close_date)]
    (if (nil? end)
      (assoc record :status true)
      (if (time/before? today end)
        (assoc record :status true)
        (assoc record :status false)))))

(defn parse-date
  [date]
  (if (string? date)
    (if (some? (re-find #"at" date))
      (format/parse (format/formatter "MMMM dd, YYYY h:mm a") (-> date
                                                                  (clojure.string/replace #"at" "")
                                                                  (clojure.string/replace #"\s+" " ")))
      (format/parse (format/formatter "MMMM dd, YYYY") date))
    date))

(defn print-date
  [date]
  (format/unparse
   (format/formatter "MMMM dd, YYYY")
   date))

(defn print-datetime
  [date]
  (format/unparse
   (format/formatter "MMMM dd, YYYY 'at' h:mm a")
   date))

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
