package org.botellier.server

import org.botellier.command.Command
import org.botellier.serializer.toByteArray

data class Request(val client: Client, val command: Command)

class RequestDispatcher(val server: Server) {
    fun dispatch(request: Request) {
        try {
            val writer = request.client.socket.getOutputStream()
            val result = request.command.execute(server.store)
            if (result != null) {
                writer.write(result.toByteArray())
            }
        }
        catch (e: Command.CommandDisabledException) {
            val writer = request.client.socket.getOutputStream().writer()
            writer.write("-COMMANDERR ${e.message}\r\n")
            writer.flush()
        }
    }

}