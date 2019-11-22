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

(defn- read-until-space [^Reader in]
  (loop [buf (CharBuffer/allocate 128)]
    (let [c (.read in)]
      (if (or (= c 32) (= c -1))
        (-> (.flip buf) (.toString))
        (recur (.put buf (char c)))))))

(defn- handle-request [handler ^Reader in ^Writer out opts]
  (loop [type nil]
    (when type
      (let [content (when (or (= type :conversion) (= type :completion))
                      (read-until-space in))
            res (res/make-response type out)]
        (try
          (prn :content content)
          (handler type content res)
          (catch Throwable _
            (when-not (res/responded? res)
              (send res "0"))))))
    (let [c (.read in)]
      (when (>= c 0)
        (case c
          48 nil
          49 (recur :conversion)
          50 (recur :version)
          51 (recur :host)
          52 (recur :completion)
          (recur nil))))))

(defn wrap-standard [handler opts]
  (fn [type content res]
    (case type
      :version (emit res "clj-skkserv.0.1 ")
      :host (emit res "127.0.0.1:1178 ")
      (handler type content res))))

(defn start-server
  ([handler] (start-server handler {}))
  ([handler {:keys [port] :or {port 1178} :as opts}]
   (let [handler' (-> handler (wrap-standard opts))]
     (server/start-server handler' opts))))

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
