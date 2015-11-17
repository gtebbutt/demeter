(ns demeter.settings
  (:require [demeter.util :refer [env parse-int]]))

(def prerender-port (env "PRERENDER_PORT" 4000))
(def log-level (env "LOG_LEVEL" "info"))
(def default-timeout (parse-int (env "DEFAULT_TIMEOUT" 5000)))
(def js-page-timeout (parse-int (env "JS_PAGE_TIMEOUT")))
