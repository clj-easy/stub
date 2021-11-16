(ns clj-easy.stub.internal-generator
  "This code runs on another shell process spawned by stub")

(defn ^:private sanitize-meta [meta]
  (-> meta
      (select-keys [:ns :name :arglists :doc :file :line :column])
      (update :ns ns-name)))

(doseq [namespace *command-line-args*]
  (require (symbol namespace)))

(let [namespaces (all-ns)]
  (->> namespaces
       (map ns-publics)
       (mapcat vals)
       (map (comp sanitize-meta meta))
       pr-str
       println))
