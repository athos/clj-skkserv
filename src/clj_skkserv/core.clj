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

(defn send [^Writer out ^String s]
  (.append out s)
  (.flush out))

(defn read-until-space [^Reader in]
  (loop [buf (CharBuffer/allocate 128)]
    (let [c (.read in)]
      (if (or (= c 32) (= c -1))
        (-> (.flip buf) (.toString))
        (recur (.put buf (char c)))))))

(defn -main []
  (let [server (ServerSocket. 8080)]
    (with-open [socket (.accept server)]
      (let [in (BufferedReader. (InputStreamReader. (.getInputStream socket) "EUC-JP"))
            out (OutputStreamWriter. (.getOutputStream socket) "EUC-JP")]
        (loop []
          (let [c (.read in)]
            (when (nat-int? c)
              (let [disconnect? (case c
                                  48 true
                                  49 (let [s (read-until-space in)]
                                       (send out "1/ほげ/ふが/\n"))
                                  50 (send out "clj-skkserv.0.1 ")
                                  51 (send out "127.0.0.1:8080 ")
                                  52 (let [s (read-until-space in)]
                                       (send out "4\n")))]
                (when-not disconnect?
                  (recur))))))))))
