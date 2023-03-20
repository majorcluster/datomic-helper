# datomic-helper

A Clojure library with handful tools for helping the usage of datomic-free

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.majorcluster/datomic-helper.svg)](https://clojars.org/org.clojars.majorcluster/datomic-helper)

## Usage

* Add the dependency:
```clojure
[org.clojars.majorcluster/datomic-helper "LAST RELEASE NUMBER"]
```
* `datomic-api` for test purposes uses [Datomic Free](https://github.com/alexanderkiel/datomic-free) (:warning: outdated)
* Feel free to integrate with your Datomic cloud or dev-local from updated datomic:
    * `connect` is received as `conn` argument by the all the functions, just pass your desired datomic version as client to your local `connect` function
    * Be aware that currently, the lib is tested just over datomic-free api

## Publish
### Requirements
* Leiningen (of course 😄)
* GPG (mac => brew install gpg)
* Clojars account
* Enter clojars/tokens page in your account -> generate one and use for password
```shell
export GPG_TTY=$(tty) && lein deploy clojars
```

## Migration
[Migration guide](./doc/MIGRATION.md)
