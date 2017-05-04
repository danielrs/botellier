package org.botellier.server

import org.botellier.command.CommandParser
import org.botellier.command.Lexer
import org.botellier.command.Parser
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * Class for handling a new client connection. It reads the input,
 * tries to parse a command, and then sends back the constructed
 * request using the provided callback.
 * @property db the current db the client is connected to.
 */
class ClientHandler(val socket: Socket, val callback: (Request) -> Unit) : Runnable {
    var db: String? = null
    override fun run() {
        println("Handling client ${socket.inetAddress.hostAddress}")
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var text = reader.readText()
            val command = CommandParser.parse(Lexer(text).lex())
            callback(Request(socket, command))

        }
        catch (e: Throwable) {
            println(e.message)
            when (e) {
                is Parser.ParserException,
                is Lexer.LexerException,
                is CommandParser.InvalidCommandException-> {
                    val write = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    write.write("-Command not recognized\r\n")
                    write.flush()
                }
            }
            socket.close()
        }
    }
}
