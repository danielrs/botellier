package org.botellier.server

import org.botellier.command.*
import org.botellier.serializer.toByteArray

data class Request(val client: Client, val command: Command)

class RequestDispatcher(val server: Server) {
    fun dispatch(request: Request) {
        val writer = request.client.socket.getOutputStream()

        try {
            if (request.command is AuthCommand) {
                val result = request.command.execute(server, request.client)
                writer.write(result.toByteArray())
            }
            else if (request.command is QuitCommand) {
                val result = request.command.execute(server, request.client)
                writer.write(result.toByteArray())
                request.client.socket.close()
            }
            else if (!server.requiresPassword() || request.client.isAuthenticated) {
                when (request.command) {
                    is ConnCommand -> {
                        val result = request.command.execute(server, request.client)
                        writer.write(result.toByteArray())
                    }
                    is StoreCommand -> {
                        val result = request.command.execute(server.dbs[request.client.dbIndex])
                        writer.write(result.toByteArray())
                    }
                    else -> {
                        throw Command.CommandException("Invalid command.")
                    }
                }
            }
            else {
                throw Command.CommandException("Not authenticated. Use AUTH command.")
            }
        }
        catch (e: Command.WrongTypeException) {
            writer.write("-WRONGTYPE ${e.message}\r\n".toByteArray())
        }
        catch (e: Throwable) {
            writer.write("-ERROR ${e.message}\r\n".toByteArray())
        }
    }
}