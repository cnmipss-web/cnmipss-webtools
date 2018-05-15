(ns webtools.exceptions.procurement)

(defn wordpress-upload-failed [filename]
  (ex-info (str "Error uploading " filename " to public website.  Please contact the webmaster.")
           {:filename filename
            :err-type :wordpress-upload}))
