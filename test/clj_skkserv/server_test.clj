(ns clj-skkserv.server-test
  (:require [clj-skkserv.server :as server]
            [clojure.test :refer [deftest are testing]]))

(defn- wrap-handler [handler]
  (let [handler (server/request-handler handler {:address "127.0.0.1" :port 1178})]
    (fn [input]
      (with-out-str
        (with-in-str input
          (handler *in* *out*))))))

(deftest request-handler-test
  (testing "conversion"
    (let [handler (wrap-handler
                   (fn [type content]
                     (when (= type :conversion)
                       ({"あいさつ" ["挨拶"]
                         "てんこう" ["転校" "天候"]}
                        content))))]
      (are [input output] (= output (handler input))
        "1あいさつ " "1/挨拶/\n"
        "1あいかつ " "4\n"
        "1てんこう " "1/転校/天候/\n"
        "4てんこう " "4\n"))))
