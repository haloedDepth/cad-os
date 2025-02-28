(ns cad-os.utils.logger
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; Define log levels as keywords
(def log-levels
  {:debug :debug
   :info :info
   :warn :warn
   :error :error
   :fatal :fatal})

;; Default minimum log level
(def ^:dynamic *min-log-level* :info)

;; Format context data for logging
(defn format-context
  "Format context data for logging output"
  [context]
  (if (and context (not (empty? context)))
    (str " | Context: "
         (try
           (with-out-str (pp/pprint context))
           (catch Exception _
             (str context))))
    ""))

;; Format exceptions for logging
(defn format-exception
  "Format an exception for logging output"
  [exception]
  (when exception
    (let [stack-trace (with-out-str
                        (.printStackTrace exception (java.io.PrintWriter. *out*)))]
      (str "\nException: " (.getMessage exception)
           "\nStack trace:\n" stack-trace))))

;; Format structured log messages
(defn format-log-message
  "Format a structured log message"
  [message context exception]
  (str message
       (format-context context)
       (format-exception exception)))

;; Main logging functions
(defn debug
  "Log a debug message"
  ([message] (debug message nil nil))
  ([message context] (debug message context nil))
  ([message context exception]
   (when (#{:debug} *min-log-level*)
     (log/debug (format-log-message message context exception)))))

(defn info
  "Log an info message"
  ([message] (info message nil nil))
  ([message context] (info message context nil))
  ([message context exception]
   (when (#{:debug :info} *min-log-level*)
     (log/info (format-log-message message context exception)))))

(defn warn
  "Log a warning message"
  ([message] (warn message nil nil))
  ([message context] (warn message context nil))
  ([message context exception]
   (when (#{:debug :info :warn} *min-log-level*)
     (log/warn (format-log-message message context exception)))))

(defn error
  "Log an error message"
  ([message] (error message nil nil))
  ([message context] (error message context nil))
  ([message context exception]
   (when (#{:debug :info :warn :error} *min-log-level*)
     (log/error (format-log-message message context exception)))))

(defn fatal
  "Log a fatal message"
  ([message] (fatal message nil nil))
  ([message context] (fatal message context nil))
  ([message context exception]
   (when (#{:debug :info :warn :error :fatal} *min-log-level*)
     (log/fatal (format-log-message message context exception)))))

;; Utility for getting a namespace-specific logger
(defmacro get-logger
  "Get a logger for a specific namespace"
  []
  `(let [namespace# (str *ns*)]
     {:debug (fn [& args#]
               (apply debug args#))
      :info (fn [& args#]
              (apply info args#))
      :warn (fn [& args#]
              (apply warn args#))
      :error (fn [& args#]
               (apply error args#))
      :fatal (fn [& args#]
               (apply fatal args#))}))

;; Set the minimum log level
(defn set-min-log-level!
  "Set the minimum log level"
  [level]
  (alter-var-root #'*min-log-level* (constantly level)))