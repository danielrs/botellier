package org.botellier.server

import org.botellier.command.Command

data class Request(val client: Client, val command: Command)
