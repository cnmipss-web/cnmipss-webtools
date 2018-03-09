(ns webtools.components.forms.generic
  "Generic, reusable form components")

(defn upload-group [{:keys [id accept multiple label class]}]
  "Creates a Bootstrap form group with col-xs-WIDTH containing a label and file 
  upload input.  Accepts a map with the following values:

    :id -- The id and name of the file upload element
    :accept -- The file type(s) accepted
    :multiple -- Whether multiple files are accepted
    :label -- Text content of the label element
    :class -- CSS classes to be applied to the group in addition to .form-group"
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
