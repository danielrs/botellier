package org.botellier.server

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket

class Server(port: Int = 6679) {
    val port: Int = port

    fun start() {
        val serverSocket = ServerSocket(port)
        println("Server running on port $port.")

        while (true) {
            val client = Client(serverSocket.accept())
            println("Client connected: ${client.socket.inetAddress.hostAddress}")

            Thread(ClientHandler(client, { client, request ->
                println("Command received: ${request.command}")
                val writer = BufferedWriter(OutputStreamWriter(client.socket.getOutputStream()))
                writer.write("$request\n")
                writer.flush()
//                writer.close()
            })).start()
        }
    }
}
