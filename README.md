[zookeeper]: https://github.com/apache/zookeeper

[![Build Status](https://travis-ci.org/danielrs/botellier.svg?branch=replication)](https://travis-ci.org/danielrs/botellier)

## Botellier

A distributed key-value data store. It aims to be a simple Redis clone for the JVM.

### Replication Scheme

Single-leader replication scheme is used. More specifically, a semi-synchronous replication scheme, where one node is the leader, another node is synchronously replicated to keep up with the leader, and the rest of the nodes are asynchronously replicated.

When the leader dies, the synced replica takes its place; when the synced replica dies or becomes leader, the most up-to-date asynchronous node takes the spot of the synced replica. Here's a flowchart that shows the election proccess:

![Flowchart](https://raw.githubusercontent.com/danielrs/botellier/master/doc/leader_election_flowchart.png)

Coordination between proccesses is done using [Zookeper][zookeeper].

### Command support

As of now, most commonly used commands are supported, such as connection, strings, lists and general ones like delete,
rename, etc. For a complete list of supported commands check [here](https://github.com/danielrs/botellier/blob/master/src/main/kotlin/org/botellier/command/commands.kt).

### Testing

The project includes an entry point that starts a server on the default port (6679) with the
password set to 'password'. After starting the server try [corkscrew](https://github.com/danielrs/corkscrew) for
connecting to it and executing commands. Here's an example:

[![asciicast](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre.png)](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre)
