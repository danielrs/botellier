[zookeeper]: https://github.com/apache/zookeeper

[![Build Status](https://travis-ci.org/danielrs/botellier.svg?branch=replication)](https://travis-ci.org/danielrs/botellier)

## Botellier

A distributed key-value data store. It aims to be a simple Redis clone for the JVM.

### Replication Scheme

To keep things simple, Botellier uses single-leader replication. Each replica is a whole copy of the leader, and no sharding/partitioning is used.

All the nodes that form the quorum can be either three types: `leader`, `synced replica` or `replica`. All writes request go through the leader, who waits for the synced replica to proccess the same requests before returning to the client. All other replicas process the requests asynchronously.

#### Election process

Initially, all nodes are of type `replica` which then try to become the `synced replica`, usually the most up-to-date node is the one to do it. After that, the only node that can become a `leader` is a synced replica; so in the event that the leader dies, the synced replica automatically becomes the leader, and the next most up-to-date common replica takes its place.

Here's a flowchart that shows the process:

![Flowchart](https://raw.githubusercontent.com/danielrs/botellier/replication/doc/leader_election_flowchart.png)

Coordination between proccesses is done using [Zookeper][zookeeper].

### Command support

As of now, most commonly used commands are supported, such as connection, strings, lists and general ones like delete,
rename, etc. For a complete list of supported commands check [here](https://github.com/danielrs/botellier/blob/master/src/main/kotlin/org/botellier/command/commands.kt).

### Testing

The project includes an entry point that starts a server on the default port (6679) with the
password set to 'password'. After starting the server try [corkscrew](https://github.com/danielrs/corkscrew) for
connecting to it and executing commands. Here's an example:

[![asciicast](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre.png)](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre)
