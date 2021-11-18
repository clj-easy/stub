(ns clj-easy.stub.main-test
  (:require
   [clj-easy.stub.main :as main]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(def default-root (.getAbsolutePath (io/file "src")))

(deftest parse
  (testing "parsing options"
    (testing "classpath"
      (is (= nil (:classpath (:options (#'main/parse [])))))
      (is (= "a:b:c" (:classpath (:options (#'main/parse ["generate" "--classpath" "a:b:c"])))))
      (is (= "a:b:c" (:classpath (:options (#'main/parse ["generate" "-c" "a:b:c"])))))
      (is (= nil (:classpath (:options (#'main/parse ["-c"]))))))
    (testing "namespaces"
      (is (= [] (:namespaces (:options (#'main/parse ["generate"])))))
      (is (= '["abc"] (:namespaces (:options (#'main/parse ["generate" "--namespaces" "abc"])))))
      (is (= '["abc"] (:namespaces (:options (#'main/parse ["generate" "-n" "abc"])))))
      (is (= '["abc" "bcd"] (:namespaces (:options (#'main/parse ["generate" "-n" "abc" "-n" "bcd"]))))))
    (testing "output-dir"
      (is (= default-root (.getAbsolutePath (:output-dir (:options (#'main/parse ["generate" "--output-dir" "src"]))))))
      (is (= default-root (.getAbsolutePath (:output-dir (:options (#'main/parse ["generate" "-o" "src"]))))))
      (is (= nil (:project-root (:options (#'main/parse ["generate" "-o" "1"])))))
      (is (= nil (:project-root (:options (#'main/parse ["generate" "p" "/this/is/not/a/valid/path"])))))))
  (testing "commands"
    (is (string? (:exit-message (#'main/parse []))))
    (is (= "generate" (:action (#'main/parse ["generate"])))))
  (testing "final options"
    (is (string? (:exit-message (#'main/parse ["--help"]))))
    (is (string? (:exit-message (#'main/parse ["-h"]))))
    (is (string? (:exit-message (#'main/parse ["--version"]))))))
