(ns clj-easy.stub.core
  (:require
   [clj-easy.stub.utils :as utils]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as string])
  (:import
   (java.io File)))

(set! *warn-on-reflection* true)

(defn ^:private internal-generator-code []
  (slurp (io/resource "clj_easy/stub/internal_generator.clj")))

(defn ^:private tmp-file []
  (File/createTempFile "clj-easy-stub." ".clj"))

(defn ^:private meta->stub
  [{:keys [ns name doc arglists file line column] :as meta}]
  (let [function-macro (if arglists
                         "defn"
                         "def")
        file-ext (if file (last (string/split file #"\.")) "clj")
        metadata (utils/assoc-some
                  {:clj-easy/stub true}
                  :line line
                  :column column
                  :file file)]
    {:ns ns
     :filename (-> ns
                   (string/replace "." (System/getProperty "file.separator"))
                   (string/replace "-" "_")
                   (str "." file-ext))
     :declaration (str "(" function-macro (str " ^" metadata) " " name
                       (if doc (str " \"" doc "\"") "")
                       (if arglists
                         (->> arglists
                              (map #(str "(" % ")"))
                              (string/join " ")
                              (str " "))
                         "")
                       ")\n")}))

(defn ^:private metas->stubs-by-filename [metas]
  (->> metas
       (map meta->stub)
       (reduce (fn [stubs-map {:keys [ns filename declaration]}]
                 (if-let [existing-content (get stubs-map filename)]
                   (assoc stubs-map filename (str existing-content declaration))
                   (let [ns-macro "in-ns"
                         ns-content (format "(%s %s)\n"
                                            ns-macro
                                            (if (= "in-ns" ns-macro)
                                              (str "'" ns)
                                              ns))]
                     (assoc stubs-map filename (str ns-content declaration)))))
               {})))

(defn ^:private spit-stubs! [output-dir stubs-by-filename]
  (mapv (fn [[filename content]]
          (let [output-file ^File (io/file output-dir filename)]
            (io/make-parents output-file)
            (spit (.getAbsolutePath output-file) content)))
        stubs-by-filename))

;; TODO Create a better API usage, probably in some other ns
(defn generate! [{:keys [classpath namespaces output-dir]
                  :or {output-dir (io/file "stubs")}}]
  (let [script-tmp-file ^File (tmp-file)
        _ (spit script-tmp-file (internal-generator-code))
        {:keys [out err exit]} (apply sh
                                      "java" "-cp" classpath
                                      "clojure.main"
                                      (.getAbsolutePath script-tmp-file)
                                      namespaces)]
    (if (= exit 0)
      (let [metas (edn/read-string out)]
        (->> metas
             metas->stubs-by-filename
             (spit-stubs! output-dir))
        {:result-code 0
         :message (str "Stubs generated sucessfully on " (.getAbsolutePath ^File output-dir))})
      {:result-code exit
       :message (or (and (not (string/blank? err))
                         err)
                    out)})))
