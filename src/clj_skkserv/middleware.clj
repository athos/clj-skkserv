(ns clj-skkserv.middleware)

(defn wrap-conversion-only [handler]
  (fn [type content]
    (when (identical? type :conversion)
      (handler type content))))

(defn wrap-completion-only [handler]
  (fn [type content]
    (when (identical? type :completion)
      (handler type content))))
