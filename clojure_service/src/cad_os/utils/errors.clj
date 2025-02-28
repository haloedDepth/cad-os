(ns cad-os.utils.errors
  (:require [cad-os.utils.logger :as logger]
            [clojure.string :as str]
            [ring.util.response :as response]))

;; Define error categories
(def error-categories
  {:client-error "CLIENT_ERROR"          ;; Client made a mistake (4xx)
   :server-error "SERVER_ERROR"          ;; Server failed (5xx)
   :validation-error "VALIDATION_ERROR"  ;; Invalid input data
   :resource-error "RESOURCE_ERROR"      ;; Resource not found/unavailable
   :unknown-error "UNKNOWN_ERROR"})      ;; Catch-all

;; Logger instance
(def log (logger/get-logger))

;; Create a standard error response
(defn error-response
  "Create a standard error response map"
  ([status message]
   (error-response status message nil nil))
  ([status message category]
   (error-response status message category nil))
  ([status message category details]
   (let [category-str (get error-categories category (:unknown-error error-categories))]
     (-> (response/response
          {:status "error"
           :category category-str
           :message message
           :detail details})
         (response/status status)))))

;; Helper functions for common error responses
(defn not-found-error
  "Create a 404 Not Found error response"
  ([message]
   (not-found-error message nil))
  ([message details]
   (error-response 404 message :resource-error details)))

(defn validation-error
  "Create a 422 Validation Error response"
  ([message]
   (validation-error message nil))
  ([message details]
   (error-response 422 message :validation-error details)))

(defn bad-request-error
  "Create a 400 Bad Request error response"
  ([message]
   (bad-request-error message nil))
  ([message details]
   (error-response 400 message :client-error details)))

(defn server-error
  "Create a 500 Internal Server Error response"
  ([message]
   (server-error message nil))
  ([message details]
   (error-response 500 message :server-error details)))

;; Exception handling middleware
(defn wrap-exception-handling
  "Middleware to catch and handle exceptions"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [error-id (str (java.util.UUID/randomUUID))
              error-message (.getMessage e)
              stack-trace (with-out-str
                            (.printStackTrace e (java.io.PrintWriter. *out*)))]

          ;; Log the error with details
          ((:error log) (str "Unhandled exception: " error-message)
                        {:error-id error-id
                         :uri (:uri request)
                         :method (:request-method request)
                         :params (:params request)}
                        e)

          ;; Return a standardized error response
          (server-error
           (str "An unexpected error occurred [" error-id "]")
           {:error_id error-id}))))))

;; Utility function for creating structured exceptions
(defn create-exception
  "Create an exception with additional metadata"
  [message type details cause]
  (let [ex (if cause
             (ex-info message {:type type :details details} cause)
             (ex-info message {:type type :details details}))]
    ;; Log the exception
    ((:error log) message {:type type :details details} ex)
    ex))

;; Helper functions for throwing specific exceptions
(defn throw-validation-error
  "Throw a validation error exception"
  ([message]
   (throw-validation-error message nil nil))
  ([message details]
   (throw-validation-error message details nil))
  ([message details cause]
   (throw (create-exception message :validation-error details cause))))

(defn throw-not-found-error
  "Throw a not found error exception"
  ([message]
   (throw-not-found-error message nil nil))
  ([message details]
   (throw-not-found-error message details nil))
  ([message details cause]
   (throw (create-exception message :not-found details cause))))

(defn throw-server-error
  "Throw a server error exception"
  ([message]
   (throw-server-error message nil nil))
  ([message details]
   (throw-server-error message details nil))
  ([message details cause]
   (throw (create-exception message :server-error details cause))))

;; Function to handle ex-info exceptions in routes
(defn handle-exception
  "Handle an ex-info exception and convert to a response"
  [e]
  (let [data (ex-data e)
        message (.getMessage e)
        type (:type data)
        details (:details data)]

    (case type
      :validation-error (validation-error message details)
      :not-found (not-found-error message details)
      :server-error (server-error message details)
      ;; Default case - treat as server error
      (server-error (str "Unexpected error: " message)
                    (if details details {})))))