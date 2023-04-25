(defproject org.clojars.majorcluster/datomic-helper "3.1.0"
  :description "A Clojure Library with tools to help using datomic"
  :url "https://github.com/majorcluster/datomic-helper"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [prismatic/schema "1.4.1"]
                 [com.datomic/datomic-free "0.9.5697"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]]
  :profiles {:dev {:plugins [[com.github.clojure-lsp/lein-clojure-lsp "1.3.17"]]}}
  :aliases {"diagnostics"     ["clojure-lsp" "diagnostics"]
            "format"          ["clojure-lsp" "format" "--dry"]
            "format-fix"      ["clojure-lsp" "format"]
            "clean-ns"        ["clojure-lsp" "clean-ns" "--dry"]
            "clean-ns-fix"    ["clojure-lsp" "clean-ns"]
            "lint"            ["do" ["diagnostics"]  ["format"] ["clean-ns"]]
            "lint-fix"        ["do" ["format-fix"] ["clean-ns-fix"]]})
