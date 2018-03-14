(ns webtools.components.forms.generic
  "Generic, reusable components used to build forms for other components")

(defn search-group
  "Creates a Bootstrap form group containing a search input and its associated label.
  Accepts a map with the following values:

    :id         -- The id of the search input
    :label      -- The label to apply to the search input
    :on-change  -- Event handler to run when the value of the search input changes
    :hide-label -- Boolean value indicating whether to make label sr-only and use 
                   placeholder text in search input instead.
  "
  [{:keys [id label on-change hide-label]}]
  [:div.form-group
   [:label {:for id
            :class (if hide-label "sr-only")} label]
   [:input.form-control {:type "search"
                         :id id
                         :placeholder (if hide-label label)
                         :on-change on-change}]])

(defn upload-group
  "Creates a Bootstrap form group with col-xs-WIDTH containing a label and file 
  upload input.  Accepts a map with the following values:

    :id -- The id and name of the file upload element
    :accept -- The file type(s) accepted
    :multiple -- Whether multiple files are accepted
    :label -- Text content of the label element
    :class -- CSS classes to be applied to the group in addition to .form-group"
  [{:keys [id accept multiple label class]}] 
  [:div.form-group
   {:class class}
   [:label.upload-form__label
    {:for id}
    label]
   [:br]
   [:input.upload-form__input
    {:type "file"
     :id id
     :name id
     :accept accept
     :multiple multiple}]])

(defn hidden-input
  "Creates a Bootstrap form group with containing a hidden-input and label for that input.
  Accepts a map with the following values:

    :name  -- Name of the input field
    :value -- the hidden input field's value (e.g. a CSRF token)
    :type  -- the type of the hidden input.  Typically text.
  "
  [{:keys [name value type]}]
  [:div {:style {:display "none"}}
   [:label {:for (str "input-" name) :aria-hidden "true"} "Hidden input"]
   [:input {:aria-hidden "true"
            :on-change nil
            :type type
            :id (str "input-" name)
            :name name
            :value value}]])

(defn csrf-token
  "A form input containing the csrf-token supplied by the server."
  []
  [hidden-input {:name "csrf-token"
                 :value js/csrfToken
                 :type "text"}])

(defn upload-form
  "Multipurpose upload form"
  [{:keys [path action accept label multiple]}]
  [:form.upload-form {:action action :method "post" :enc-type "multipart/form-data"}
   [:div.row
    [:div.col-xs-6
     [upload-group {:id "file"
                    :accept accept
                    :multiple multiple
                    :label label}]]
    [:div.col-xs-6
     [hidden-input {:name "path"
                    :value path
                    :type "text"}]
     [csrf-token]
     [:button#upload-btn.btn.btn-primary.form-control {:type "submit"
                                                       :aria-label "Upload"
                                                       :style {:width "100%"
                                                               :height "100%"}} "Upload"]]]])
