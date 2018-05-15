(ns webtools.components.forms.hro
  (:require [webtools.components.forms.generic :as gforms]
            [webtools.handlers.events :as events]))

(defn jva-upload []
  [gforms/upload-form {:path (.-hash js/location)
                       :action "/webtools/upload/jva-pdf"
                       :accept ".pdf"
                       :label "Upload New JVA"
                       :multiple true}])

(defn search-jva-records []
  [:form.search-form {:role "search"}
   [gforms/search-group {:id "search-jvas"
                         :label "Search JVAs"
                         :on-change events/search-jvas
                         :hide-label true}]])
