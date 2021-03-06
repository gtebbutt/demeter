(ns demeter.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [>! <! chan close!]]
            [demeter.logging :as log]
            [demeter.settings :as settings]))

(def request (atom (nodejs/require "requestretry")))

(def default-opts (atom {}))

(defn get-url-direct
  [url {:keys [buffer? timeout] :as opts}]
  (log/debug "Getting " url)
  (let [channel (chan)
        params (merge
                 {:method "GET"
                  :uri url
                  :gzip true
                  :timeout (or timeout settings/default-timeout)}
                 @default-opts)]
    (@request
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

(def prerender-prefix (str "http://localhost:" settings/prerender-port "/"))

(defn get-js-url-direct
  [url opts]
  (get-url-direct (str prerender-prefix url)
                  (merge {:timeout settings/js-page-timeout}
                         opts)))

(defn init-scrapers!
  ([concurrency input-chan]
   (init-scrapers! concurrency input-chan false nil))
  ([concurrency input-chan js?]
   (init-scrapers! concurrency input-chan js? nil))
  ([concurrency input-chan js? rate-limit]
   (dotimes [_ concurrency]
     (go-loop
       []
       (when rate-limit
         (<! (cljs.core.async/timeout rate-limit)))
       (when-let [input (<! input-chan)]
         ;TODO: Schema to ensure input has a URL and a resp-chan
         (let [scrape-fn (if js? get-js-url-direct get-url-direct)
               output-chan (:output-chan input)
               resp (<! (scrape-fn (:url input) (:opts input)))]
           (>! output-chan
               (merge input resp))
           (when (:close-output? input) (close! output-chan)))
         (recur))))))

(defn init-js-scrapers!
  ([concurrency input-chan]
   (init-js-scrapers! concurrency input-chan nil))
  ([concurrency input-chan rate-limit]
   (init-scrapers! concurrency input-chan true rate-limit)))

(defn get-url
  ([url input-chan]
   (get-url url nil input-chan))
  ([url opts input-chan]
   (let [channel (chan 1)]
     (go
       (>! input-chan {:url url :output-chan channel :opts opts :close-output? true}))
     channel)))

(defn get-image
  [url input-chan]
  (go
    (let [resp (<! (get-url url {:buffer? true} input-chan))]
      (when (= (:status resp) 200)
        (:body resp)))))
