(ns clj-easy.stub.core)

(defn generate! [{:keys [classpath namespaces output-dir]}]
  (println (format "Generating stubs for classpath %s with given entrypoints %s to %s"
                   classpath
                   namespaces
                   output-dir)))
