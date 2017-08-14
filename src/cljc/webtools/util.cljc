(ns webtools.util)

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
