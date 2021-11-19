(ns clj-easy.stub.core-test
  (:require
   [clj-easy.stub.core :as core]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(defn code [& strings] (string/join "\n" strings))

(def success-single-ns-metas
  [{:ns 'some.ns
    :name 'something
    :doc "Some cool doc"
    :arglists ["a" "b"]
    :file "src/some/ns.clj"
    :line 2
    :column 3}])

(def success-multiple-ns-metas
  [{:ns 'some.ns
    :name 'something
    :doc "Some cool doc"
    :arglists ["a" "b"]
    :file "src/some/ns.clj"
    :line 2
    :column 3}
   {:ns 'some.ns
    :name 'otherthing
    :doc "Some other cool doc"
    :file "src/some/ns.clj"
    :line 5
    :column 6}
   {:ns 'another.cool-ns
    :name 'foo
    :doc "Some foo doc"
    :file "src/another/cool_ns.clj"
    :line 10
    :column 2}])

(deftest generate!
  (testing "When classpath is not passed"
    (is (thrown? AssertionError (= nil (core/generate! {})))))
  (testing "When namespaces is not passed or empty"
    (is (thrown? AssertionError (= nil (core/generate! {:classpath "foo"}))))
    (is (thrown? AssertionError (= nil (core/generate! {:classpath "foo"
                                                        :namespaces []})))))
  (testing "When java command returns an error"
    (with-redefs [core/create-script-tmp-file! (constantly (io/file "tmp"))
                  shell/sh (constantly {:out ""
                                        :err "Some error"
                                        :exit 1})]
      (is (= {:result-code 1
              :message "Some error"}
             (core/generate! {:classpath "foo:bar"
                              :namespaces ["some.ns"]})))))
  (testing "single namespace"
    (testing "When java command returns success edn meta"
      (with-redefs [core/create-script-tmp-file! (constantly (io/file "tmp"))
                    shell/sh (constantly {:out (str success-single-ns-metas)
                                          :err ""
                                          :exit 0})
                    io/make-parents (constantly nil)
                    spit (constantly nil)]
        (is (= {:result-code 0
                :stubs {"some/ns.clj" (code "(in-ns 'some.ns)"
                                            "(defn ^{:clj-easy/stub true, :line 2, :column 3, :file \"src/some/ns.clj\"} something \"Some cool doc\" (a) (b))"
                                            "")}
                :message "Stubs generated and persisted sucessfully"}
               (core/generate! {:classpath "foo:bar"
                                :namespaces ["some.ns"]})))))
    (testing "When java command returns success edn meta for dry?"
      (with-redefs [core/create-script-tmp-file! (constantly (io/file "tmp"))
                    shell/sh (constantly {:out (str success-single-ns-metas)
                                          :err ""
                                          :exit 0})
                    io/make-parents (constantly nil)]
        (is (= {:result-code 0
                :stubs {"some/ns.clj" (code "(in-ns 'some.ns)"
                                            "(defn ^{:clj-easy/stub true, :line 2, :column 3, :file \"src/some/ns.clj\"} something \"Some cool doc\" (a) (b))"
                                            "")}
                :message "Stubs generated sucessfully"}
               (core/generate! {:classpath "foo:bar"
                                :dry? true
                                :namespaces ["some.ns"]}))))))
  (testing "multiple namespaces"
    (testing "with multiple vars"
      (with-redefs [core/create-script-tmp-file! (constantly (io/file "tmp"))
                    shell/sh (constantly {:out (str success-multiple-ns-metas)
                                          :err ""
                                          :exit 0})
                    io/make-parents (constantly nil)
                    spit (constantly nil)]
        (is (= {:result-code 0
                :stubs {"some/ns.clj" (code "(in-ns 'some.ns)"
                                            "(defn ^{:clj-easy/stub true, :line 2, :column 3, :file \"src/some/ns.clj\"} something \"Some cool doc\" (a) (b))"
                                            "(def ^{:clj-easy/stub true, :line 5, :column 6, :file \"src/some/ns.clj\"} otherthing \"Some other cool doc\")"
                                            "")
                        "another/cool_ns.clj" (code "(in-ns 'another.cool-ns)"
                                                    "(def ^{:clj-easy/stub true, :line 10, :column 2, :file \"src/another/cool_ns.clj\"} foo \"Some foo doc\")"
                                                    "")}
                :message "Stubs generated and persisted sucessfully"}
               (core/generate! {:classpath "foo:bar"
                                :namespaces ["some.ns"]})))))))
