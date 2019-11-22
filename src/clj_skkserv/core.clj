(ns clj-skkserv.core
  (:refer-clojure :exclude [send])
  (:import [java.io
            BufferedReader
            InputStreamReader
            OutputStreamWriter
            Reader
            Writer]
           [java.net ServerSocket]
           [java.nio CharBuffer]))

(defn- send [^Writer out ^String s]
  (.append out s)
  (.flush out))

(defmacro thread [& body]
  `(doto (Thread. (fn [] ~@body))
     (.start)))

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
                      (read-until-space in))]
        (prn :content content)
        (handler type content out)))
    (let [c (.read in)]
      (when (>= c 0)
        (case c
          48 nil
          49 (recur :conversion)
          50 (recur :version)
          51 (recur :name)
          52 (recur :completion)
          (recur nil))))))

(defn wrap-standard [handler opts]
  (fn [type content out]
    (case type
      :version (send out "clj-skkserv.0.1 ")
      :name (send out "127.0.0.1:8080 ")
      (handler type content out))))

(defn start-server
  ([handler] (start-server handler {}))
  ([handler {:keys [port] :or {port 1178} :as opts}]
   (println "starting server ...")
   (let [socket (ServerSocket. port)]
     (loop []
       (let [conn (.accept socket)
             in (-> (.getInputStream conn)
                    (InputStreamReader. "EUC-JP")
                    (BufferedReader.))
             out (-> (.getOutputStream conn) (OutputStreamWriter. "EUC-JP"))
             handler' (-> handler (wrap-standard opts))]
         (println "accepted new connection")
         (when-not (.isClosed socket)
           (thread
             (try
               (handle-request handler' in out opts)
               (finally
                 (.close conn)
                 (println "disconnected connection"))))
           (recur))))
     socket)))

(defn hiragana->katakana [s]
  (let [re #"[\u3040-\u309F]"]
    (when (re-find re s)
      (-> s
          (clojure.string/replace re #(str (char (+ (int (first %)) 96))))
          (clojure.string/replace #"[a-z]$" "")))))

(defn -main []
  (letfn [(conv [s]
            (case s
              "/time" (str (java.util.Date.))))
          (handler [type content out]
            (case type
              :conversion (send out
                                (if-let [katakana (hiragana->katakana content)]
                                  (format "1/%s/\n" katakana)
                                  "1\n"))
              :completion (send out "4\n")))]
    (start-server handler)))
