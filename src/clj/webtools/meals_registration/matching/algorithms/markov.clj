(ns webtools.meals-registration.matching.algorithms.markov
  (:require [clj-fuzzy.metrics :as smetric]
            [clojure.string :as s]))

(defn- -prefix-val-list [size coll]
  (let [prefixes (butlast (map vec (partition size 1 coll)))
        values   (drop size coll)]
        (map vector prefixes values)))

(defn- -multi-prefix-val-list [size list-words]
  (apply conj (-prefix-val-list size (first list-words))
         (mapcat (partial -prefix-val-list size) (next list-words))))

(defn create-counts [size coll]
  "Computes how many times did each 'next state' come from a 'previous state'.
  Order must be < (count coll).
  The result type is {previous_state {next_state count}}."
  (let [zipped  (mapcat #(-multi-prefix-val-list % coll) (range 1 (+ 1 size)))
        sorted  (sort zipped)
        grouped (group-by first sorted)
        seconds (map (fn [[key pairs]] [key (map second pairs)]) (seq grouped))
        freqs   (map (fn [[key secs]]  [key (frequencies secs)]) seconds)]
    (into {} freqs)))

(defn- -get-names [records]
  (reduce #(apply conj %1 [(:first-name %2) (:last-name %2)]) [] records))


(defn seed [fns-records nap-records]
  (let [all-names (->> (apply conj
                              (-get-names fns-records)
                              (-get-names nap-records))
                       (map clojure.string/lower-case))
        longest (count (last all-names))]
    (create-counts longest all-names)))

(defn create-totals [count-map]
  "Computes the number of occurences of each state.
   The result type is {state count}."
  (let [totals (map (fn [[key counts]] [key (apply + (vals counts))])
                    (seq count-map))]
    (into {} totals)))

(defn create-probs [count-map]
  "Computes the probabilities of each of the transitions.
   That's done by normalizing their counts into interval <0,1>.
   The result type is {previous_state {next_state probability}}."
  (let [totals (create-totals count-map)
        probs  (map (fn [[key counts]]
                      (let [the-total (get totals key)]
                        [key (into {} (map (fn [[k c]] [k (/ c the-total)])
                                           (seq counts)))]))
                    (seq count-map))]
    (into {} probs)))

(defn probs-splitter [probs name]
  (for [i (range 1 (count name))]
    (let [prefix (apply vector (take i name))
          value (first (first (partition 1 (get (s/split name #"") i))))]
      [prefix value (get-in probs [prefix value])])))

(defn compare-names [probs name1 name2]
  (let [seq1 (probs-splitter probs name1)
        seq2 (probs-splitter probs name2)]
    [seq1 seq2 (smetric/mra-comparison name1 name2)]))

(defn likely-match [] )
