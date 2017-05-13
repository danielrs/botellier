[![Build Status](https://travis-ci.org/danielrs/botellier.svg?branch=master)](https://travis-ci.org/danielrs/botellier)

## Botellier

A wannabe, distributed key-value data store. It aims to be a simple Redis clone for the JVM.

### Testing

The project includes an entry point that starts a server on the default port (6679) with the
password set to 'password'. After starting the server try [corkscrew](https://github.com/danielrs/corkscrew) for
connecting to it and executing commands. As of now, there's support for most common commands (connection, strings, lists
and general commands such as delete, rename, etc). Here's an example:

[![asciicast](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre.png)](https://asciinema.org/a/b5yhrwnsu8v4rkna08yoa3wre)

For a lists of all supported commands check [here](https://github.com/danielrs/botellier/blob/master/src/main/kotlin/org/botellier/command/commands.kt).
