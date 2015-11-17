(ns demeter.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [>! <! chan close!]]
            [demeter.logging :as log]
            [demeter.settings :as settings]))

(def request (nodejs/require "requestretry"))

(defn get-url-direct
  [url {:keys [buffer? timeout] :as opts}]
  (log/debug "Getting " url)
  (let [channel (chan)
        params {:method "GET"
                :uri url
                :gzip true
                :timeout (or timeout settings/default-timeout)}]
    (request
     (clj->js
      (if buffer?
        (assoc params :encoding nil)
        params))
     (fn [err resp body]
       (go
        (if err
          (>! channel {:error err
                       :input-url url})
          (>! channel {:status (.-statusCode resp)
                       :url (.. resp -request -uri -href)
                       :input-url url
                       :headers (js->clj (.-headers resp) :keywordize-keys true)
                       :body body}))
        (close! channel))))
    channel))

(defn get-js-url-direct
  [url opts]
  (get-url-direct (str "http://localhost:" settings/prerender-port "/" url)
                  (merge {:timeout settings/js-page-timeout}
                         opts)))

(defonce default-input-chan (chan))

(defn init-scrapers!
  ([concurrency]
   (init-scrapers! concurrency default-input-chan false))
  ([concurrency input-chan]
   (init-scrapers! concurrency input-chan false))
  ([concurrency input-chan js?]
   (dotimes [_ concurrency]
     (go-loop
      []
      (when-let [input (<! input-chan)]
        ;TODO: Schema to ensure input has a URL and a resp-chan
        (let [scrape-fn (if js? get-js-url-direct get-url-direct)
              resp (<! (scrape-fn (:url input) (:opts input)))]
          (>! (:output-chan input)
              (merge input resp)))
        (recur))))))

(defn init-js-scrapers!
  ([concurrency]
   (init-scrapers! concurrency default-input-chan true))
  ([concurrency input-chan]
   (init-scrapers! concurrency input-chan true)))

(defn get-url
  ([url]
   (get-url url nil default-input-chan))
  ([url opts]
   (get-url url opts default-input-chan))
  ([url opts input-chan]
   (let [channel (chan 1)]
     (go
      (>! input-chan {:url url :output-chan channel :opts opts})
      (close! channel))
     channel)))

(defn get-image
  ([url]
   (get-image url default-input-chan))
  ([url input-chan]
   (go
    (let [resp (<! (get-url url {:buffer? true}))]
      (when (= (:status resp) 200)
        (:body resp))))))
