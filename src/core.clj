(ns core
  (:require [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [malli.core :as mall]
            [interceptors :as i]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:gen-class))

(def PORT 8080)


(defn health-handler
  "Handler para health check - retorna status da API"
  [request]
  (let [start-time (get-in request [:app :start-time] (java.time.Instant/now))
        uptime (-> (java.time.Instant/now)
                   (.toEpochMilli)
                   (- (.toEpochMilli start-time))
                   (quot 1000))
        response-body {:status "OK"
                       :message "API is running"
                       :timestamp (java.time.Instant/now)
                       :version "0.1.0"
                       :uptime uptime}]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str response-body)}))

(defn hello-handler
  "Handler para hello world - retorna saudação personalizada"
  [request]
  (let [name (get-in request [:coerced-body :name] "World")
        greeting-type (if (str/blank? name) "casual" "friendly")
        response-body {:message (str "Hello " name "!")
                       :timestamp (java.time.Instant/now)
                       :greeting-type greeting-type}]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str response-body)}))


(defn health-handler-with-middleware [request]
  (let [request-with-meta (-> request
                              (assoc :timestamp (java.time.Instant/now))
                              (assoc :request-id (str (java.util.UUID/randomUUID))))]
    (println (str "🔍 Request: " (:request-method request) " " (:uri request)
                 " | ID: " (:request-id request-with-meta)
                 " | Time: " (:timestamp request-with-meta)))
    (try
      (health-handler request-with-meta)
      (catch Exception e
        (println (str "❌ Error in health-handler: " (.getMessage e)))
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "Internal server error"
                                :message (.getMessage e)})}))))

(defn hello-handler-with-middleware [request]
  (let [request-with-meta (-> request
                              (assoc :timestamp (java.time.Instant/now))
                              (assoc :request-id (str (java.util.UUID/randomUUID))))]
    (println (str "🔍 Request: " (:request-method request) " " (:uri request)
                 " | ID: " (:request-id request-with-meta)
                 " | Time: " (:timestamp request-with-meta)))
    (try
      (hello-handler request-with-meta)
      (catch Exception e
        (println (str "❌ Error in hello-handler: " (.getMessage e)))
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "Internal server error"
                                :message (.getMessage e)})}))))

(def routes
  ["/api"
   
   ;; Health Check Endpoint
   ["/health"
    {:get {:summary "Health check endpoint - returns API status and uptime"
           :description "Returns the health status of the API with uptime information"
           :tags ["health" "monitoring"]
           :responses {200 {:description "API is healthy"
                           :body {:status "OK"
                                  :message "API is running"
                                  :timestamp "2024-01-15T10:30:00Z"
                                  :version "0.1.0"
                                  :uptime 3600}}}
           :handler health-handler-with-middleware}}]
   
   ;; Hello World Endpoint
   ["/hello"
    {:get {:summary "Hello World endpoint - returns personalized greeting"
           :description "Returns a greeting message, optionally personalized with a name"
           :tags ["greeting" "demo"]
           :responses {200 {:description "Greeting message"
                           :body {:message "Hello World!"
                                  :timestamp "2024-01-15T10:30:00Z"
                                  :greeting-type "casual"}}}
           :handler hello-handler-with-middleware}}]])



(def app
  (ring/ring-handler
   (ring/router routes)
   (ring/routes
    ;; Endpoint para documentação da API (similar ao ISA do Nubank)
    (ring/create-resource-handler {:path "/docs"})
    ;; Default handler para 404
    (ring/create-default-handler))))

;; Função principal de entrada. Inicia o Jetty.
(defn -main
  "Inicia o servidor web com interceptors no padrão Nubank."
  [& args]
  (println (str "🚀 Servidor Clojure com Interceptors (padrão Nubank) iniciado na porta " PORT "..."))
  (println (str "📚 Documentação: http://localhost:" PORT "/docs"))
  (println (str "🔍 Health Check: http://localhost:" PORT "/api/health"))
  (println (str "👋 Hello World: http://localhost:" PORT "/api/hello"))
  (println (str "📋 Schema Validation: Malli com interceptors"))
  (println (str "🎯 Padrão: Clojure-native (sem OpenAPI/Swagger)"))
  (jetty/run-jetty app {:port PORT :join? false}))
