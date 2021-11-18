(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def lib 'clj-easy/stub)
(def version (string/trim (slurp (io/resource "STUB_VERSION"))))
(def class-dir "target/classes")
(def basis {:project "deps.edn"})
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [opts]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber [opts]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :main 'clj-easy.stub.main
           :basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))}))

(defn native [opts]
  (uber (merge opts {:extra-aliases [:native]}))
  (if-let [graal-home (System/getenv "GRAALVM_HOME")]
    (let [command (->> [(str (io/file graal-home "bin" "native-image"))
                        "-jar" uber-file
                        "-H:+ReportExceptionStackTraces"
                        "--verbose"
                        "--no-fallback"
                        "--native-image-info"
                        (or (System/getenv "STUB_XMX")
                            "-J-Xmx4g")
                        (when (= "true" (System/getenv "STUB_STATIC"))
                          "--static")]
                       (remove nil?))
          {:keys [exit]} (b/process {:command-args command})]
      (System/exit exit))
    (println "Set GRAALVM_HOME env")))

(defn deploy [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
