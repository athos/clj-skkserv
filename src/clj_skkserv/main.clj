(ns clj-skkserv.main
  (:require [clj-skkserv.core :as skkserv]
            [clojure.string :as str]))

(defn- parse-opts [args]
  (->> (partition 2 args)
       (into {} (map (fn [[k v]] [(keyword (str/replace k "--" "")) v])))))

(defn -main [& args]
  (let [{:keys [handler port] :as opts} (parse-opts args)]
    (if handler
      (if-let [handler-var (resolve (symbol handler))]
        (let [opts' (-> (dissoc opts :handler)
                        (cond-> port (update :port #(Long/parseLong %))))]
          (skkserv/start-server @handler-var opts'))
        (throw (ex-info (str "Specified handler (" handler ") was not found") {})))
      (throw (ex-info "--handler must be specified" {})))))
