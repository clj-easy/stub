(ns clj-easy.stub.internal-generator
  "This code runs on another shell process spawned by stub.
  It requires the namespaces at runtime.")

(defn ^:private sanitize-meta [meta]
  (-> meta
      (select-keys [:ns :name :arglists :doc :file :line :column])
      (update :ns ns-name)))

(let [require-ns (->> *command-line-args*
                      (map symbol)
                      set)]
  (doseq [namespace require-ns]
    (require namespace))
  (->> (all-ns)
       (filter #(contains? require-ns (ns-name %)))
       (map ns-publics)
       (mapcat vals)
       (map (comp sanitize-meta meta))
       pr-str
       println))
