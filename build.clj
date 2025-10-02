(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'my-clojure-healthcheck/core) ; Nome do namespace principal
(def version "0.0.1")                  ; Versão
(def class-dir "target/classes")       ; Diretório para compilação
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (println "Limpando diretórios...")
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil) ; Limpa antes de construir

  (println "Compilando...")
  (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})

  (println "Criando Uberjar...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file jar-file
           :basis basis
           :main 'core}) ; Define o namespace principal como o entry point

  (println (str "Uberjar criado: " jar-file)))