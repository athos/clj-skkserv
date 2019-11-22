(ns clj-skkserv.response
  (:refer-clojure :exclude [flush])
  (:import [java.io Writer]))

(defrecord Response [type ^Writer out responded])

(defn make-response [type out]
  (->Response type out (atom false)))

(defn responded? [res]
  @(:responded res))

(defn emit [{:keys [^Writer out] :as res} ^String s]
  (.append out s)
  res)

(defn flush [{:keys [^Writer out] :as res}]
  (.flush out)
  res)

(defn respond [{:keys [type ^Writer out] :as res} coll]
  (when-not (responded? res)
    (if (empty? coll)
      (emit res "4\n")
      (let [sep (case type
                  :conversion "/"
                  :completion " ")]
        (emit res "1")
        (emit res sep)
        (doseq [x coll]
          (emit res x)
          (emit res sep))
        (emit res "\n")))
    (flush res)
    (reset! (:responded res) true)
    true))
