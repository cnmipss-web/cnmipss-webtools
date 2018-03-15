(ns webtools.sass)

(def ^:private sass-config
  {:input "resources/scss/screen.scss"
   :output "resources/public/css/screen.css"
   :executable-path "sass"})

(def ^:private sass-process (atom nil))

(defn start! []
  (let [{:keys [executable-path input output]} sass-config]
    (reset! sass-process
            (.exec (Runtime/getRuntime)
                   (str executable-path " --watch --scss " input ":" output)))
    (println "Figwheel: Starting SASS watch process")))

(defn stop! []
  (when-let [process @sass-process]
    (println "Figwheel: Stopping SASS watch process")
    (reset! sass-process (.destroy process))))
