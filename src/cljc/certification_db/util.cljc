(ns certification-db.util)

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

#?(:cljs (do
           (def jq js/jQuery)))
