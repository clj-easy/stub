(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def lib 'clj-easy/stub)
(def current-version (string/trim (slurp (io/resource "STUB_VERSION"))))
(def class-dir "target/classes")
(def basis {:project "deps.edn"})
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) current-version))
(def jar-file (format "target/%s-%s.jar" (name lib) current-version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [opts]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version current-version
                :basis (b/create-basis (update basis :aliases concat (:extra-aliases opts)))
                :src-dirs ["src/lib"]})
  (b/copy-dir {:src-dirs ["src/lib"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber [opts]
  (clean nil)
  (let [default-aliases [:cli]]
    (b/copy-dir {:src-dirs ["src/lib" "src/cli" "resources/cli"]
                 :target-dir class-dir})
    (b/compile-clj {:basis (b/create-basis (update basis :aliases concat default-aliases (:extra-aliases opts)))
                    :src-dirs ["src/lib" "src/cli"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :main 'clj-easy.stub.main
             :basis (b/create-basis (update basis :aliases concat default-aliases (:extra-aliases opts)))})))

(defn native [opts]
  (if-let [graal-home (System/getenv "GRAALVM_HOME")]
    (let [jar (or (System/getenv "STUB_JAR")
                  (do (uber (merge opts {:extra-aliases [:native]}))
                      uber-file))
          command (->> [(str (io/file graal-home "bin" "native-image"))
                        "-jar" jar
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

(defn ^:private replace-in-file [file regex content]
  (as-> (slurp file) $
    (string/replace $ regex content)
    (spit file $)))

(defn tag [{:keys [version]}]
  {:pre [(string? version)]}
  (b/process {:command-args ["git" "fetch" "origin"]})
  (b/process {:command-args ["git" "pull" "origin" "HEAD"]})
  (replace-in-file "pom.xml"
                   (str "<version>" current-version "</version>")
                   (str "<version>" version "</version>"))
  (replace-in-file "pom.xml"
                   (str "<tag>v" current-version "</tag>")
                   (str "<tag>v" version "</tag>"))
  (replace-in-file "CHANGELOG.md"
                   #"## Unreleased"
                   (format "## Unreleased\n\n## %s" (name version)))
  (replace-in-file "resources/lib/STUB_VERSION"
                   current-version
                   version)
  (b/process {:command-args ["git" "add" "pom.xml" "CHANGELOG.md" "resources/lib/STUB_VERSION"]})
  (b/process {:command-args ["git" "commit" "-m" (str "\"Release: " version "\"")]})
  (b/process {:command-args ["git" "tag" (str "v" version)]})
  (b/process {:command-args ["git" "push" "origin" "HEAD"]})
  (b/process {:command-args ["git" "push" "origin" "HEAD" "--tags"]}))

(defn deploy-clojars [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
