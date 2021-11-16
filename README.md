# stub

A tool for generating stubs for open and closed source libraries.

## Instalation

TODO

## Usage

To generate stubs you need to pass the `classpath` and at least one namespace from `namespaces` to `stub` later require it and find all related namespaces for stub generation.
You can specify a optional `output-dir`, otherwise the `stubs` folder will be used.
For more details check `stub --help`.

### CLI

`stub --classpath ".../clojure.jar:/foo/bar.jar" --namespaces foo.bar`

`stub -c ".../clojure.jar:/foo/bar.jar" -n foo.bar -o /tmp/stubs`

### API

TODO

## License

Copyright © 2021 clj-easy maintainers

Distributed under the Eclipse Public License version 1.0.
