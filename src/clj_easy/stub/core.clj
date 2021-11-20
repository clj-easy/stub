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
  [{:keys [ns name doc arglists file line column]}]
  (let [definition-macro (if arglists
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
     :declaration (str "(" definition-macro (str " ^" metadata) " " name
                       (if doc (str " \"" (string/escape doc {\" "\\\""}) "\"") "")
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

(defn ^:private create-script-tmp-file! []
  (let [script-tmp-file ^File (tmp-file)]
    (spit script-tmp-file (internal-generator-code))
    script-tmp-file))

(defn generate!
  "Generate stubs for the given `classpath`, requiring `namespaces`
  and saving the namespaces hierarchy into `output-dir`.
  This function spawns a clojure program using the provided or default `java-command`.
  If `dry?` is truthy, return the stubs on the result map without creating the file hierarchy.

  Return a map with:
    `result-code` the status code of the result. Anything different from `0` means an error.
    `message` A message about the result.
    `stubs` The generated stubs if `dry?` flag is truthy."
  [{:keys [classpath namespaces output-dir java-command dry?]
    :or {output-dir (io/file "stubs")
         java-command "java"}}]
  {:pre [(instance? File output-dir)
         (string? classpath)
         (string? java-command)
         (seq namespaces)]}
  (io/make-parents output-dir)
  (when-not (.exists ^File output-dir)
    (.mkdirs ^File output-dir))
  (let [script-tmp-file ^File (create-script-tmp-file!)
        {:keys [out err exit]} (apply sh
                                      java-command "-cp" classpath
                                      "clojure.main"
                                      (.getAbsolutePath script-tmp-file)
                                      namespaces)]
    (if (= exit 0)
      (let [metas (edn/read-string out)
            stubs-by-filename (metas->stubs-by-filename metas)]
        (if dry?
          {:result-code 0
           :stubs stubs-by-filename
           :message (str "Stubs generated sucessfully")}
          (do
            (spit-stubs! output-dir stubs-by-filename)
            {:result-code 0
             :stubs stubs-by-filename
             :message (str "Stubs generated and persisted sucessfully")})))
      {:result-code exit
       :message (or (and (not (string/blank? err))
                         err)
                    out)})))
