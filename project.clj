(defproject demeter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :clean-targets ^{:protect false} ;Unprotected to allow deletion of file outside project root
  ["out-scraper" "deploy/scraper.js"]

  :profiles
  {:default [:cljs-shared]

   :cljs-shared
   {:dependencies [[org.clojure/clojure "1.7.0"]
                   [org.clojure/clojurescript "1.7.170"]
                   [org.clojure/core.async "0.2.374"]]

    :plugins [[lein-cljsbuild "1.1.1"]
              [lein-npm "0.6.1"]]

    :npm {:dependencies [[loglevel "1.4.0"]
                         [requestretry "1.5.0"]
                         [aws-sdk "2.2.12"]
                         [prerender "https://github.com/prerender/prerender.git#17388269f08deb5ece22739414e47509119d24b2"]]}

    :cljsbuild
    {:builds [{:id "scraper"
               :source-paths ["src"]
               :compiler
               {:target :nodejs
                :output-to "deploy/scraper.js"
                :output-dir "out-scraper"
                :preamble ["include.js"]
                :optimizations :simple
                :language-in :ecmascript5
                :language-out :ecmascript5}}]}}})
