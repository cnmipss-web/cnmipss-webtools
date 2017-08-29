(ns webtools.components.forms-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [reagent.core :as reagent :refer [atom]]
            [webtools.components.forms :as forms]
            [webtools.procurement.core :as p]))


(deftest test-procurement-addendum
  (let [id (random-uuid)
        form (forms/procurement-addendum (p/map->PSAnnouncement {:id id
                                                                 :type :rfp
                                                                 :number "1"}))]
    (testing "should create hidden form inputs for id, type, and number"
      (let [[_ _ _ [_ id-input number-input type-input] _] form]
        (is (= :input.form-control (first id-input)))
        (is (= "text" (-> id-input second :type)))
        (is (= id (-> id-input second :value)))

        (is (= :input.form-control (first number-input)))
        (is (= "text" (-> number-input second :type)))
        (is (= "1" (-> number-input second :value)))

        (is (= :input.form-control (first type-input)))
        (is (= "text" (-> type-input second :type)))
        (is (= :rfp (-> type-input second :value)))))))
