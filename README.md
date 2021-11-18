# stub

_A tool for generating stubs for open and closed source libraries_

## Instalation

TODO

## Usage

To generate stubs you need to pass the `classpath` and at least one namespace from `namespaces` to `stub` later require it and find all related namespaces for stub generation.
You can specify a optional `output-dir`, otherwise the `stubs` folder will be used.
For more details check `stub --help`.

After running sucessfully, a hierarchy of files and folders with all the stubs should be available with custom metadata. Example:

`stubs/foo/bar.clj`
```clojure
(in-ns 'foo.bar)
(defn ^{:clj-easy/stub true, :line 16, :column 1, :file "foo/bar.clj"} something ([]) ([a b]))
(defn ^{:clj-easy/stub true, :line 18, :column 1, :file "foo/bar.clj"} other ([]))
```

### CLI

`stub --classpath ".../clojure.jar:/foo/bar.jar" --namespaces foo.bar`

`stub -c ".../clojure.jar:/foo/bar.jar" -n foo.bar -o /tmp/stubs`

### API

TODO

## How does it work

This tool first create a temporary file with a custom clojure code, then shell out a java process with the specified classpath calling the code from the created temporary file which should get all necessary metadata and then finish, which stub should then create all files from that metadata.

## Develop

Run `clj -M:run generate -c "<your-classpath> -n some.entrypoint-ns"`

## Build

JVM

`clj -T:build uber`

GraalVM native image

`clj -T:build native`

## Deploy

`clj -T:build deploy`

## License

Copyright © 2021 clj-easy maintainers

Distributed under the Eclipse Public License version 1.0.
