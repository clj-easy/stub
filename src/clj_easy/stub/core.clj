(ns clj-easy.stub.core
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io File)))

(defn generate! [{:keys [classpath namespaces output-dir]
                  :or {output-dir (io/file "")}}]
  {:result-code 0
   :message (str "Stubs generated sucessfully on " (.getAbsolutePath ^File output-dir))})
