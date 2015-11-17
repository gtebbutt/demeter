(ns demeter.util
  (:require [cljs.nodejs :as nodejs]))

(defn env
  ([s]
   (env s nil))
  ([s default]
   (or (aget nodejs/process "env" s) default)))

(defn parse-int
  [s]
  (let [n (js/parseInt s)]
    (when-not (js/isNaN n)
      n)))
