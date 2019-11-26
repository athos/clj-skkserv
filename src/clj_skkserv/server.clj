(ns clj-skkserv.server
  (:require [clj-skkserv.response :as res])
  (:import [java.io
            BufferedReader
            Closeable
            InputStreamReader
            OutputStreamWriter
            Reader
            Writer]
           [java.net InetAddress ServerSocket]
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

(defn- emit [res content]
  (-> res
      (res/emit content)
      (res/flush)))

(defn- handle-fn
  [handler {:keys [address port] :or {address "localhost" port 1178}}]
  (let [version "clj-skkserv.0.1 "
        host (str address \: port \space)]
    (fn [^Reader in ^Writer out]
      (loop [type nil]
        (when type
          (let [res (res/make-response type out)]
            (case type
              (:conversion :completion)
              (try
                (let [content (read-until-space in)]
                  (prn :content content)
                  (res/respond res (handler type content)))
                (catch Throwable _
                  (when-not (res/responded? res)
                    (emit res "0"))))
              :version (emit res version)
              :host (emit res host)
              nil)))
        (let [c (.read in)]
          (when (>= c 0)
            (case c
              48 nil
              49 (recur :conversion)
              50 (recur :version)
              51 (recur :host)
              52 (recur :completion)
              (recur nil))))))))

(defmacro thread [& body]
  `(doto (Thread. (fn [] ~@body))
     (.start)))

(defn start-server [handler {:keys [address port] :as opts}]
  (println "starting server ...")
  (let [address (InetAddress/getByName address)
        socket (ServerSocket. port 0 address)
        handle (handle-fn handler opts)]
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
              (handle in out)
              (finally
                (.close conn)
                (println "disconnected connection"))))
          (recur))))
    (->SkkServer socket)))
