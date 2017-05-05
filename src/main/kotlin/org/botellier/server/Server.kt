package org.botellier.server

import org.botellier.store.Store
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket

class Server(port: Int = 6679) {
    val store: Store = Store()
    val port: Int = port

    fun start() {
        val serverSocket = ServerSocket(port)
        println("Server running on port $port.")

        while (true) {
            val clientSocket = serverSocket.accept()
            println("Client connected: ${clientSocket.inetAddress.hostAddress}")

            Thread(ClientHandler(clientSocket, {
                println("Command received: $it")
                val writer = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
                writer.write("$it\n")
                writer.flush()
//                writer.close()
            })).start()
        }
    }
}
