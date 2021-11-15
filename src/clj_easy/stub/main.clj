(ns clj-easy.stub.main
  (:refer-clojure :exclude [run!])
  (:gen-class)
  (:require
   [clj-easy.stub.core :as core]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.cli :as t.cli]))

(set! *warn-on-reflection* true)

(defn ^:private version []
  (->> [(str "stub TODO")]
       (string/join \newline)))

(defn ^:private help [options-summary]
  (->> ["Clojure tool for generating stubs for a classpath."
        ""
        "Usage: stub <command> [<options>]"
        ""
        "All options:"
        options-summary
        ""
        "Available commands:"
        "  generate             Generate the stubs."
        ""
        ;; "Run \"stub help <command>\" for more information about a command."
        "See https://github.com/clj-easy/stub for detailed documentation."]
       (string/join \newline)))

(defn ^:private cli-options []
  [["-h" "--help" "Print the available commands and its options"]
   [nil "--version" "Print stub version"]

   ["-cp" "--classpath CLASSPATH" "The classpath string used to search for libraries to then generate the stubs."
    :id :classpath
    :validate [string? "Specify a valid classpath string after --classpath"]]

   ["-n" "--namespaces NS" "Namespaces to require to later then generate the stubs. This flag accepts multiple values"
    :id :namespaces
    :default []
    :parse-fn symbol
    :multi true
    :update-fn conj]

   ["-o" "--output-dir DIR" "The directory to spit out the generated stubs."
    :id :output
    :parse-fn io/file
    :validate [#(-> % io/file .exists) "Specify a valid directory path after --output-dir"]]])

(defn ^:private error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn ^:private parse [args]
  (let [{:keys [options arguments errors summary]} (t.cli/parse-opts args (cli-options))]
    (cond
      (:help options)
      {:exit-message (help summary) :ok? true}

      (:version options)
      {:exit-message (version) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (and (= 1 (count arguments))
           (#{"generate"} (first arguments)))
      {:action (first arguments) :options options}

      :else
      {:exit-message (help summary)})))

(defn ^:private exit [status msg]
  (when msg
    (println msg))
  (System/exit status))

(defn ^:private with-required-options [options required fn]
  (doseq [option required]
    (when-not (get options option)
      (exit 1 (format "Missing required %s option for this command. Check stub --help for more details." option))))
  (when (every? options required)
    (apply fn [options])))

(defn ^:private handle-action!
  [action options]
  (case action
    "generate" (with-required-options
                 options
                 [:classpath :namespaces]
                 core/generate!)))

(defn run! [& args]
  (let [{:keys [action options exit-message ok?]} (parse args)]
    (if exit-message
      {:result-code (if ok? 0 1)
       :message exit-message}
      (handle-action! action options))))

(defn -main [& args]
  (let [{:keys [result-code message]} (apply run! args)]
    (exit result-code message)))
