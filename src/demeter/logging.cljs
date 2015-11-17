(ns demeter.logging
  (:require [cljs.nodejs :as nodejs]))

(def logger (nodejs/require "loglevel"))

(defn trace
  [& msg]
  (.trace logger (apply str msg)))

(defn debug
  [& msg]
  (.debug logger (apply str msg)))

(defn info
  [& msg]
  (.info logger (apply str msg)))

(defn warn
  [& msg]
  (.warn logger (apply str msg)))

(defn error
  [& msg]
  (.error logger (apply str msg)))

(defn set-level!
  [level]
  (.setLevel logger level))
