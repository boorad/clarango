;; Please note:
;; The methods in this namespace are only intended for internal use.

(ns clarango.utilities.http-utility
	(:require [clj-http.client :as http]
		        [cheshire.core :refer :all])
  (:use clojure.pprint))

(defn- build-server-exception-string
  [error]
  (str "Clarango: There was an error trying to access a resource: " 
    (.getMessage error) 
    "; body: " 
    (parse-string (:body (:object (.getData error))))))

(defn- build-connection-exception-string
  [error]
  (str (.getMessage error)
    "; There is probably something wrong with the server. "
    "Is the server running and did you set the right connection-url?"))

(defmulti handle-error
  "Handle an error trying to access a resource."
  (fn [error] (type error)))
(defmethod handle-error java.net.ConnectException
  [error]
  (throw (Exception. (build-connection-exception-string error))))
(defmethod handle-error clojure.lang.ExceptionInfo
  [error]
  (throw (Exception. (build-server-exception-string error))))
(defmethod handle-error :default
  [error]
  (throw error))
;; TO DO: later create custom exceptions for Clarango?

(defn debugging-activated? []
  "Switch that activates the verbose output of clj-http."
  false)

(defn console-output-activated? []
  "Switch that activates outputting the http method and url used for each http request."
  true)

(defn get-uppercase-string-for-http-method
  "Returns a string of uppercase letters with the name of the matching http method.
  Pass it a method name as symbol, e.g. :post"
  [method]
  (case method
    :get "GET"
    :head "HEAD"
    :post "POST"
    :put "PUT"
    :patch "PATCH"
    :delete "DELETE"))

(defn get-head-delete-uri [method uri params]
  (if (console-output-activated?) (println (get-uppercase-string-for-http-method method) " connection address: " uri))
  (try (let [opts {:debug (debugging-activated?) :query-params params}
              response (http/request (merge {:method method :url uri} opts))]
        (if (= method :head) 
          (:headers response)
          (parse-string (:body response))))
        (catch Exception e (handle-error e))))

(defn get-uri [uri params]
  (get-head-delete-uri :get uri params))

(defn head-uri [uri params]
  (get-head-delete-uri :head uri params))

(defn delete-uri [uri params]
  (get-head-delete-uri :delete uri params))

(defn post-put-patch-uri
  "Since post, put and patch have the same set of arguments and only differ in the used http method, this is a meta
  method for using all of these.
  The first argument must be on of these http methods in form of a symbol, e.g. :post"
  [method uri body params]
  (if (console-output-activated?) (println (get-uppercase-string-for-http-method method) " connection address: " uri))
  (try (let [opts {:debug (debugging-activated?) :query-params params :body (generate-string body)}
              response (http/request (merge {:method method :url uri} opts))]
        (parse-string (:body response)))
        (catch Exception e (handle-error e))))

(defn post-uri [uri body params]
  (post-put-patch-uri :post uri body params))

(defn put-uri [uri body params]
  (post-put-patch-uri :put uri body params))

(defn patch-uri [uri body params]
  (post-put-patch-uri :patch uri body params))