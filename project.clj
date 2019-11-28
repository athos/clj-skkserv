(defproject clj-skkserv "0.1.0-SNAPSHOT"
  :description "An skkserv server framework in Clojure, heavily inspired by Ring architecture"
  :url "https://github.com/athos/clj-skkserv"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "0.5.0"]]
  :repl-options {:init-ns clj-skkserv.core})
