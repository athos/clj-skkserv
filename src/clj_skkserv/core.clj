(ns clj-skkserv.core
  (:refer-clojure :exclude [send])
  (:import [java.io OutputStream InputStream]
           [java.net ServerSocket]))

(defn send-raw [^OutputStream out ^bytes bytes]
  (.write out bytes)
  (.flush out))

(defn send [out ^String s]
  (send-raw out (.getBytes s "EUC-JP")))

(defn read-until-space [^InputStream in ^bytes buf]
  (loop [offset 0]
    (let [b (.read in)]
      (if (or (= b 32) (= b -1))
        (String. buf 0 offset "EUC-JP")
        (do (aset buf offset (unchecked-byte b))
            (recur (inc offset)))))))

(defn -main []
  (let [server (ServerSocket. 8080)]
    (with-open [socket (.accept server)]
      (let [in (.getInputStream socket)
            out (.getOutputStream socket)
            buf (byte-array 256)]
        (loop []
          (let [b (.read in)]
            (when (nat-int? b)
              (print (char b))
              (flush)
              (let [disconnect? (case b
                                  48 true
                                  49 (let [s (read-until-space in buf)]
                                       (println :conversion s)
                                       (send out "1/ほげ/ふが/\n"))
                                  50 (send-raw out (.getBytes "clj-skkserv.0.1 "))
                                  51 (send-raw out (.getBytes "127.0.0.1:8080 "))
                                  52 (let [s (read-until-space in buf)]
                                       (println :complement s)
                                       (send out "4\n")))]
                (when-not disconnect?
                  (recur))))))))))
