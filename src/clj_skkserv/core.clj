(ns clj-skkserv.core
  (:require [clj-skkserv.server :as server]))

(defn start-server
  ([handler] (start-server handler {}))
  ([handler {:keys [port] :as opts}]
   (let [opts' (cond-> opts (nil? port) (assoc :port 1178))]
     (server/start-server handler opts'))))
