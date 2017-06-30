[![Build Status](https://travis-ci.org/danielrs/botellier.svg?branch=replication)](https://travis-ci.org/danielrs/botellier)

## Botellier

A distributed key-value data store. It aims to be a simple Redis clone for the JVM.

### Command support

As of now, most commonly used commands are supported, such as connection, strings, lists and general ones like delete,
rename, etc. For a complete list of supported commands check [here](https://github.com/danielrs/botellier/blob/master/src/main/kotlin/org/botellier/command/commands.kt).

### Testing

The project includes an entry point that starts a server on the default port (6679) with the
password set to 'password'. After starting the server try [corkscrew](https://github.com/danielrs/corkscrew) for
connecting to it and executing commands. Here's an example:

[![asciicast](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre.png)](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre)
