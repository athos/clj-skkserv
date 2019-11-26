(ns clj-skkserv.core
  (:require [clj-skkserv.server :as server]))

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
  (letfn [(handler [type content]
            (case type
              :conversion (when-let [katakana (hiragana->katakana content)]
                            [katakana])
              nil))]
    (start-server handler)))
