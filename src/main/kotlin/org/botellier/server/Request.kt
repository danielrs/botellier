package org.botellier.server

import org.botellier.command.Command
import org.botellier.serializer.toByteArray

data class Request(val client: Client, val command: Command)

class RequestDispatcher(val server: Server) {
    fun dispatch(request: Request) {
        val byteWriter = request.client.socket.getOutputStream()
        val writer = byteWriter.writer()

        try {
            val result = request.command.execute(server.store)
            println(result)
            byteWriter.write(result.toByteArray())
        }
        catch (e: Command.CommandException) {
            writer.write("-ERROR ${e.message}\r\n")
        }
        catch (e: Command.CommandDisabledException) {
            writer.write("-ERROR ${e.message}\r\n")
        }
        catch (e: Command.WrongTypeException) {
            writer.write("-WRONGTYPE ${e.message}\r\n")
        }
        catch (e: Throwable) {
            writer.write("-ERROR ${e.message}\r\n")
        }

        writer.flush()
    }

}