# earl

A clojure project that lets you visually inspect running [ordasity](https://github.com/boundary/ordasity) clusters

![earl](http://f.cl.ly/items/0z0L0V040y172G1t0A17/Image%202014-03-28%20at%203.11.30%20PM.png)

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

## Extras

There's a hubot plugin in `extras/earl_hubot.coffee`. It assumes your earl deployment is behind https basic auth (you shouldn't ever use http basic auth - passwords are sent in plain text)

## License

Copyright © 2014 Tom Crayford

Distributed under the Eclipse Public License version 1.0
