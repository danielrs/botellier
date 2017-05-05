package org.botellier.server

import org.botellier.command.CommandParser
import org.botellier.command.Lexer
import org.botellier.command.Parser
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Class for handling a new client connection. It reads the input,
 * tries to parse a command, and then sends back the constructed
 * request using the provided callback.
 * @property db the current db the client is connected to.
 */
class ClientHandler(val socket: Socket, val callback: (Request) -> Unit) : Runnable {
    var db: String? = null
    var readTimeout: Int = 1000

    override fun run() {
        println("Handling client ${socket.inetAddress.hostAddress}")
        loop@while (true) {
            try {
                val stream = socket.waitInput()

                socket.soTimeout = readTimeout
                val tokens = Lexer(stream).lex()
                socket.soTimeout = 0

                val command = CommandParser.parse(tokens)
                callback(Request(socket, command))
            }
            catch (e: SocketException) {
                break@loop
            }
            catch (e: Throwable) {
                println(e.message)
                val writer = socket.getOutputStream().bufferedWriter()
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
                        socket.close()
                        break@loop
                    }
                }
                writer.flush()
            }
        }
    }
}
