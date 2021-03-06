package org.botellier.server

import org.botellier.command.*
import org.botellier.serializer.toByteArray
import org.botellier.store.StoreException

data class Request(val client: Client, val command: Command)

class RequestDispatcher(val server: Server) {
    fun dispatch(request: Request) {
        val writer = request.client.socket.getOutputStream()
        val isAuthenticated = !server.requiresPassword() || request.client.isAuthenticated
        try {
            when {
                request.command is AuthCommand -> {
                    val result = request.command.execute(server, request.client)
                    writer.write(result.toByteArray())
                }
                request.command is QuitCommand -> {
                    val result = request.command.execute(server, request.client)
                    writer.write(result.toByteArray())
                    request.client.socket.close()
                }
                isAuthenticated -> {
                    when (request.command) {
                        is ConnCommand -> {
                            val result = request.command.execute(server, request.client)
                            writer.write(result.toByteArray())
                        }
                        is ReadStoreCommand -> {
                            val store = server.dbs[request.client.dbIndex]
                            val result = request.command.execute(store)
                            writer.write(result.toByteArray())
                        }
                        is StoreCommand -> {
                            val store = server.dbs[request.client.dbIndex]
                            val result = request.command.execute(store)
                            writer.write(result.toByteArray())
                        }
                        else -> {
                            throw CommandException.RuntimeException("Invalid commands.")
                        }
                    }
                }
                else ->
                    throw CommandException.RuntimeException("Not authenticated. Use AUTH command.")
            }
        }
        catch (e: StoreException.InvalidTypeException) {
            writer.write("-WRONGTYPE ${e.message}\r\n".toByteArray())
        }
        catch (e: Throwable) {
            writer.write("-ERROR ${e.message}\r\n".toByteArray())
        }
    }
}