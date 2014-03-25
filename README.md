# earl

A clojure project that lets you visually inspect running [ordasity](github.com/boundary.ordasity) clusters

## Usage

grab an uberjar, then run it:

```
java -jar earl-uberjar.jar config-file.edn
```

Config files are just edn files. An example:

```clojure
{:earl/brand "Yeller"
 :earl/clusters
 #{"Yeller-Production" "YellerDev"}
 :earl/quote
 "2012 quality"
  :zookeeper-cluster "localhost:2181"
 :jetty-options {:port 3000}}
```

`:jetty-options` are optional (and are just passed raw to `run-jetty`). The default port is 8080.
`:zookeeper-cluster` is likewise optional, defaulting to `localhost:2181`. They are passed directly to curator, so feel free to pass a comma separated connection string to connect to multiple zk servers

## License

Copyright © 2014 Tom Crayford

Distributed under the Eclipse Public License version 1.0
