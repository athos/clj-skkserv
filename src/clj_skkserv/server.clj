(ns clj-skkserv.server
  (:require [clj-skkserv.response :as res])
  (:import [java.io
            BufferedReader
            Closeable
            InputStreamReader
            OutputStreamWriter
            Reader
            Writer]
           [java.net ServerSocket]
           [java.nio CharBuffer]))

(defrecord SkkServer [socket]
  Closeable
  (close [this]
    (.close socket)))

(defn- read-until-space [^Reader in]
  (loop [buf (CharBuffer/allocate 128)]
    (let [c (.read in)]
      (if (or (= c 32) (= c -1))
        (-> (.flip buf) (.toString))
        (recur (.put buf (char c)))))))

(defn- handle [handler ^Reader in ^Writer out opts]
  (loop [type nil]
    (let [res (res/make-response type out)]
      (case type
        (:conversion :completion)
        (let [content (read-until-space in)]
          (try
            (prn :content content)
            (handler type content res)
            (catch Throwable _
              (when-not (res/responded? res)
                (send res "0")))))
        :version (res/emit res "clj-skkserv.0.1 ")
        :host (res/emit res "127.0.0.1:1178 ")
        nil))
    (let [c (.read in)]
      (when (>= c 0)
        (case c
          48 nil
          49 (recur :conversion)
          50 (recur :version)
          51 (recur :host)
          52 (recur :completion)
          (recur nil))))))

(defmacro thread [& body]
  `(doto (Thread. (fn [] ~@body))
     (.start)))

(defn start-server [handler {:keys [port] :as opts}]
  (println "starting server ...")
  (let [socket (ServerSocket. port)]
    (loop []
      (let [conn (.accept socket)
            in (-> (.getInputStream conn)
                   (InputStreamReader. "EUC-JP")
                   (BufferedReader.))
            out (-> (.getOutputStream conn) (OutputStreamWriter. "EUC-JP"))]
        (println "accepted new connection")
        (when-not (.isClosed socket)
          (thread
            (try
              (handle handler in out opts)
              (finally
                (.close conn)
                (println "disconnected connection"))))
          (recur))))
    (->SkkServer socket)))
