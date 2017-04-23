package org.botellier.server

import org.botellier.store.Store
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
        }
    }
}
