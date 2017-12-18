(defproject cnmipss-webtools "0.2.7"

  :description "Internal Web Application for various CNMI PSS Webtools"
  :url "http://www.cnmipss.org/webtools/"

  :dependencies [[cheshire "5.7.1"]
                 [cider/cider-nrepl "0.14.0"]
                 [clj-fuzzy "0.4.0"]
                 [clj-http "3.6.1"]
                 [clj-time "0.14.0"]
                 [cljs-ajax "0.6.0"]
                 [cljsjs/jquery "3.2.1-0"]
                 [clojusc/friend-oauth2 "0.2.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [com.cemerick/friend "0.2.3"]
                 [com.cemerick/url "0.1.1"]
                 [com.draines/postal "2.0.2"]
                 [com.layerware/hugsql "0.4.7"]
                 [compojure "1.6.0"]
                 [conman "0.6.4"]
                 [cprop "0.1.10"]
                 [funcool/struct "1.0.0"]
                 [hiccup "1.0.5"]
                 [jarohen/chime "0.2.2"]
                 [klang "0.5.7"]
                 [luminus-immutant "0.2.3"]
                 [luminus-migrations "0.3.9"]
                 [luminus-nrepl "0.1.4"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [markdown-clj "0.9.99"]
                 [metosin/muuntaja "0.3.1"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.11"]
                 [org.apache.pdfbox/pdfbox "2.0.6"]
                 [org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671" :scope "provided"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.flatland/useful "0.11.5"]
                 [org.postgresql/postgresql "42.0.0"]
                 [org.webjars.bower/tether "1.4.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.5"]
                 [org.webjars/font-awesome "4.7.0"]
                 [re-frame "0.9.4"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [ring-cors "0.1.11"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.6.1"]
                 [ring/ring-defaults "0.3.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-spec "0.0.3"]
                 [secretary "1.2.3"]
                 [selmer "1.10.8"]
                 [slester/ring-browser-caching "0.1.1"]
                 [stuarth/clj-oauth2 "0.3.2"]
                 [try-let "1.1.0"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot webtools.core

  :migratus {:store :database
             :db ~(get (System/getenv) "DATABASE_URL")}
  
  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.5"]
            [lein-cprop "1.0.3"]
            [lein-immutant "2.1.0"]
            [lein-sassc "0.10.4"]
            [migratus-lein "0.4.9"]
            [lein-doo "0.1.7"]]

  :sassc
  [{:src "resources/scss/screen.scss"
    :output-to "resources/public/css/screen.css"
    :style "nested"
    :import-path "resources/scss"}] 
  
  :auto
  {"sassc" {:file-pattern #"\.(scss|sass)$" :paths ["resources/scss"]}} 
  
  :hooks []
  
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware
   [cemerick.piggieback/wrap-cljs-repl cider.nrepl/cider-middleware]}

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-to "target/cljsbuild/public/js/app.js"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}
               }}
             
             :aot :all
             :uberjar-name "webtools.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:dependencies [[binaryage/devtools "0.9.4"]

                                 [circleci/bond "0.3.0"]
                                 [com.cemerick/piggieback "0.2.2"]
                                 [doo "0.1.7"]
                                 [figwheel-sidecar "0.5.11"]
                                 [pjstadig/humane-test-output "0.8.2"]
                                 [prone "1.1.4"]
                                 [ring/ring-devel "1.6.1"]
                                 [ring/ring-mock "0.3.1"]]

                  :plugins      [[com.jakemccrary/lein-test-refresh "0.19.0"]
                                 [lein-doo "0.1.7"]
                                 [lein-figwheel "0.5.11"]
                                 [org.clojure/clojurescript "1.9.671"]
                                 [lein-cloverage "1.0.9"]]
                  
                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :figwheel {:on-jsload "webtools.core/mount-components"}
                     :compiler
                     {:preloads [devtools.preload]
                      :npm-deps {}
                      :main "webtools.app"
                      :asset-path "/webtools/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}
                    :test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs" "env/test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "webtools.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}
                  
                  :doo {:alias {:default [:phantom] 
                                :all [:chrome :firefox :safari]}
                        :build "test"}
                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "webtools.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}
   :profiles/dev {}
   :profiles/test {}})
