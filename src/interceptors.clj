(ns interceptors
  (:require [malli.core :as m]
            [malli.util :as mu]
            [clojure.string :as str]))



(def health-response-schema
  [:map
   {:title "Health Response"
    :description "Response schema for health check endpoint"}
   [:status [:enum "OK" "ERROR"]]
   [:message string?]
   [:timestamp inst?]
   [:version string?]
   [:uptime pos-int?]])

(def hello-request-schema
  [:map
   {:title "Hello Request"
    :description "Request schema for hello endpoint"}
   [:name [:maybe string?]]])

(def hello-response-schema
  [:map
   {:title "Hello Response"
    :description "Response schema for hello endpoint"}
   [:message string?]
   [:timestamp inst?]
   [:greeting-type [:enum "formal" "casual" "friendly"]]])



(defn doc-desc
  "Middleware para adicionar documentação/descrição ao endpoint"
  [description & {:keys [tags examples]}]
  (fn [handler]
    (fn [request]
      (let [request-with-doc (assoc-in request [:doc] 
                                      {:description description
                                       :tags (or tags [])
                                       :examples (or examples {})})]
        (handler request-with-doc)))))

(defn adapt-coerce!
  "Middleware para validar request body usando Malli schema"
  [request-schema]
  (fn [handler]
    (fn [request]
      (let [body (:body request)]
        (if (and request-schema body)
          (if (m/validate request-schema body)
            (handler (assoc request :coerced-body body))
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body {:error "Invalid request body"
                    :details (m/explain request-schema body)}})
          (handler request))))))

(defn adapt-externalize!
  "Middleware para validar response body usando Malli schema"
  [response-schema]
  (fn [handler]
    (fn [request]
      (let [response (handler request)
            body (:body response)]
        (if (and response-schema body)
          (if (m/validate response-schema body)
            response
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body {:error "Invalid response format"
                    :details (m/explain response-schema body)}})
          response)))))


(defn add-timestamp
  "Middleware para adicionar timestamp automático"
  []
  (fn [handler]
    (fn [request]
      (handler (assoc request :timestamp (java.time.Instant/now))))))

(defn add-request-id
  "Middleware para adicionar request ID único"
  []
  (fn [handler]
    (fn [request]
      (handler (assoc request :request-id (str (java.util.UUID/randomUUID)))))))

(defn log-request
  "Middleware para logar requests"
  []
  (fn [handler]
    (fn [request]
      (println (str "🔍 Request: " (:request-method request) " " (:uri request)
                   " | ID: " (:request-id request)
                   " | Time: " (:timestamp request)))
      (handler request))))


(defn validate-schema
  "Função utilitária para validar schemas"
  [schema data]
  (if (m/validate schema data)
    {:valid? true :data data}
    {:valid? false 
     :errors (m/explain schema data)}))

(defn schema-examples
  "Gera exemplos de dados baseados no schema"
  [schema]
  (cond
    (m/validate [:map] schema) {:example "map"}
    (m/validate [:vector] schema) {:example "vector"}
    (m/validate [:string] schema) {:example "string"}
    :else {:example "unknown"}))
