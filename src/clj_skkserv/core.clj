(ns clj-skkserv.core
  (:refer-clojure :exclude [flush send])
  (:require [clj-skkserv.response :as res]
            [clj-skkserv.server :as server])
  (:import [java.io Reader Writer]
           [java.nio CharBuffer]))

(defn emit [res s] (res/emit res s))
(defn flush [res] (res/flush res))

(defn respond
  ([res] (respond res nil))
  ([res candidates]
   (res/respond res candidates)))

(defn send [res s]
  (-> res
      (emit s)
      (flush)))

(defn start-server
  ([handler] (start-server handler {}))
  ([handler {:keys [port] :or {port 1178} :as opts}]
   (server/start-server handler opts)))

(defn hiragana->katakana [s]
  (let [re #"[\u3040-\u309F]"]
    (when (re-find re s)
      (-> s
          (clojure.string/replace re #(str (char (+ (int (first %)) 96))))
          (clojure.string/replace #"[a-z]$" "")))))

(defn -main []
  (letfn [(handler [type content res]
            (case type
              :conversion (if-let [katakana (hiragana->katakana content)]
                            (respond res [katakana])
                            (respond res))
              :completion (respond res)))]
    (start-server handler)))
