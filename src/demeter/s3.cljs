(ns demeter.s3
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [>! <! chan close!]]
            [demeter.logging :as log]
            [demeter.core :as core]))

(def aws (nodejs/require "aws-sdk"))

(def s3-constructor (.-S3 aws))
(def s3 (s3-constructor.))

(defn write
  [body filename bucket & [public?]]
  (let [channel (chan)
        params {:Bucket bucket
                :Key filename
                :Body body}]
    (.upload s3 (clj->js (if public? (assoc params :ACL "public-read") params))
             (fn [err data]
               (when err (log/error "S3 error: " err))
               (go
                (>! channel {:error err
                             :data (js->clj data :keywordize-keys true)})
                (close! channel))))
    channel))

(defn save-image
  ([url filename bucket input-chan]
   (go
    (if-let [body (<! (core/get-image url input-chan))]
      (<! (write body filename bucket :public))

      {:error :request-failed}))))
