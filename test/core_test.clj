(ns core-test
  (:require [clojure.test :refer :all]
            [core :refer :all]
            [interceptors :as i]
            [malli.core :as m]
            [ring.mock.request :as mock]))

(deftest hello-world-endpoint-test
  (testing "Hello World endpoint returns correct response with schema validation"
    (let [request (mock/request :get "/api/hello")
          response (app request)]
      (is (= 200 (:status response)))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))
      (is (contains? (:body response) :message))
      (is (= "Hello World!" (get-in response [:body :message])))
      (is (contains? (:body response) :timestamp))
      (is (contains? (:body response) :greeting-type))
      ;; Testa se o response está validado pelo schema
      (is (m/validate i/hello-response-schema (:body response))))))

(deftest hello-world-with-name-test
  (testing "Hello World endpoint with name parameter"
    (let [request (-> (mock/request :get "/api/hello")
                      (mock/content-type "application/json")
                      (mock/body "{\"name\": \"Alice\"}"))
          response (app request)]
      (is (= 200 (:status response)))
      (is (= "Hello Alice!" (get-in response [:body :message])))
      (is (= "friendly" (get-in response [:body :greeting-type]))))))

(deftest health-check-endpoint-test
  (testing "Health check endpoint returns correct response with schema validation"
    (let [request (mock/request :get "/api/health")
          response (app request)]
      (is (= 200 (:status response)))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))
      (is (contains? (:body response) :status))
      (is (= "OK" (get-in response [:body :status])))
      (is (contains? (:body response) :message))
      (is (= "API is running" (get-in response [:body :message])))
      (is (contains? (:body response) :timestamp))
      (is (contains? (:body response) :version))
      (is (contains? (:body response) :uptime))
      ;; Testa se o response está validado pelo schema
      (is (m/validate i/health-response-schema (:body response))))))

(deftest schema-validation-test
  (testing "Malli schemas are properly defined"
    (is (m/validate i/health-response-schema 
                    {:status "OK"
                     :message "API is running"
                     :timestamp (java.time.Instant/now)
                     :version "0.1.0"
                     :uptime 3600}))
    (is (m/validate i/hello-response-schema
                    {:message "Hello World!"
                     :timestamp (java.time.Instant/now)
                     :greeting-type "casual"}))
    (is (m/validate i/hello-request-schema
                    {:name "Alice"}))))

(deftest not-found-endpoint-test
  (testing "Non-existent endpoint returns 404"
    (let [request (mock/request :get "/api/nonexistent")
          response (app request)]
      (is (= 404 (:status response))))))

(deftest routes-test
  (testing "Routes are properly configured with documentation"
    (let [health-route (-> routes second second :get)
          hello-route (-> routes last second :get)]
      ;; Verifica se as rotas têm documentação
      (is (some? (:summary health-route)))
      (is (some? (:summary hello-route)))
      (is (some? (:description health-route)))
      (is (some? (:description hello-route)))
      (is (some? (:tags health-route)))
      (is (some? (:tags hello-route))))))

