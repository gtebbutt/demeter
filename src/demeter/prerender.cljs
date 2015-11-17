(ns demeter.prerender
  (:require [cljs.nodejs :as nodejs]
            [demeter.settings :as settings]))

(def prerender (nodejs/require "prerender"))

(defonce server-instance (atom))

(defn start
  [settings]
  (let [server (prerender (clj->js settings))]
    (reset! server-instance server)
    (.use server (.httpHeaders prerender))
    (.use server (.inMemoryHtmlCache prerender))
    (.start server)))

(defn stop
  []
  (.exit nodejs/process))
