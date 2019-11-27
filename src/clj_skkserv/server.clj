(ns clj-skkserv.server
  (:require [clj-skkserv.response :as res]
            [clj-skkserv.version :as ver]
            [clojure.tools.logging :as log])
  (:import [java.io
            BufferedReader
            Closeable
            InputStreamReader
            OutputStreamWriter
            Reader
            Writer]
           [java.net InetAddress ServerSocket]
           [java.nio CharBuffer]))

(defrecord SkkServer [^ServerSocket socket]
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

(defn- request-type [^long c]
  (case c
    48 :disconnect
    49 :conversion
    50 :version
    51 :host
    52 :completion
    :unknown))

(defn- handle-fn [handler {:keys [address port]}]
  (let [version (str "clj-skkserv" ver/VERSION \space)
        host (str address ": ")]
    (fn [^Reader in ^Writer out]
      (let [alive? (atom true)]
        (while @alive?
          (let [c (.read in)]
            (if (neg? c)
              (reset! alive? false)
              (let [type (request-type c)
                    res (res/make-response type out)]
                (log/debugf "received %s request" (name type))
                (case type
                  (:conversion :completion)
                  (try
                    (let [content (read-until-space in)]
                      (log/debugf "requested content: %s" content)
                      (res/respond res (handler type content)))
                    (catch Throwable t
                      (log/error t "error occurred during request handling")
                      (when-not (res/responded? res)
                        (emit res "0"))))

                  :version (emit res version)
                  :host (emit res host)
                  :disconnect (reset! alive? false)
                  nil)))))))))

(defmacro thread [& body]
  `(doto (Thread. (fn [] ~@body))
     (.start)))

(defn start-server [handler {:keys [address port] :as opts}]
  (let [address (InetAddress/getByName address)
        socket (ServerSocket. port 0 address)
        handle (handle-fn handler opts)]
    (log/infof "server started listening at %s:%d" address port)
    (loop []
      (let [conn (.accept socket)
            in (-> (.getInputStream conn)
                   (InputStreamReader. "EUC-JP")
                   (BufferedReader.))
            out (-> (.getOutputStream conn) (OutputStreamWriter. "EUC-JP"))]
        (log/info "accepted new client")
        (when-not (.isClosed socket)
          (thread
            (try
              (handle in out)
              (catch Throwable t
                (log/error "unexpected error has occurred"))
              (finally
                (.close conn)
                (log/info "closed connection to client"))))
          (recur))))
    (->SkkServer socket)))
