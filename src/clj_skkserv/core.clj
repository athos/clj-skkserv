(ns clj-skkserv.core
  (:require [clj-skkserv.server :as server]))

(defn start-server
  ([handler] (start-server handler {}))
  ([handler {:keys [address port] :as opts}]
   (let [opts' (cond-> opts
                 (nil? address) (assoc :address "127.0.0.1")
                 (nil? port) (assoc :port 1178))]
     (server/start-server handler opts'))))
