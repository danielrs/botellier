package org.botellier.server

import org.botellier.command.Command
import java.net.Socket

data class Request(val client: Socket, val command: Command) {}
