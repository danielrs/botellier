package org.botellier.server

import org.botellier.command.CommandParser
import org.botellier.command.Lexer
import org.botellier.command.Parser
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Container class for client information.
 */
data class Client(val socket: Socket, var dbIndex: Int = 0, var isAuthenticated: Boolean = false)

/**
 * Class for handling a new client connection. It reads the input,
 * tries to parse a command, and then sends back the constructed
 * request using the provided callback.
 * @property db the current db the client is connected to.
 */
class ClientHandler(val client: Client, val dispatcher: RequestDispatcher) : Runnable {
    var readTimeout: Int = 1000

    override fun run() {
        println("Handling client ${client.socket.inetAddress.hostAddress}")
        loop@while (true) {
            try {
                val stream = client.socket.waitInput()

                client.socket.soTimeout = readTimeout
                val tokens = Lexer(stream).lex()
                client.socket.soTimeout = 0

                val command = CommandParser.parse(tokens)
                dispatcher.dispatch(Request(client, command))
            }
            catch (e: SocketException) {
                break@loop
            }
            catch (e: Throwable) {
                println(e.message)
                val writer = client.socket.getOutputStream().bufferedWriter()
                when (e) {
                    // Exception for Lexer waiting too much.
                    is SocketTimeoutException ->
                        writer.write("-ERR Command read timeout\r\n")

                    // Exception regarding the serialized data.
                    is Lexer.LexerException ->
                        writer.write("-COMMANDERR Unable to read command\r\n")

                    // Exception regarding the structure of the data.
                    is Parser.ParserException ->
                        writer.write("-COMMANDERR Unable to parse command\r\n")

                    // Exception regarding unknown command.
                    is CommandParser.UnknownCommandException ->
                        writer.write("-COMMANDERR ${e.message}\r\n")

                    // Exception that we don't know how to handle.
                    else -> {
                        client.socket.close()
                        break@loop
                    }
                }
                writer.flush()
            }
        }
        println("Dropping client ${client.socket.inetAddress.hostAddress}")
    }
}
