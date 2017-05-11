package org.botellier.server

import org.botellier.store.Store
import java.net.ServerSocket
import java.util.concurrent.Executor

class Server(port: Int = 6679) {
    val port = port
    val store = Store()

    private val executor = HandlerExecutor()
    private val dispatcher = RequestDispatcher(this)

    fun start() {
        val serverSocket = ServerSocket(port)
        println("Server running on port $port.")

        while (true) {
            val client = Client(serverSocket.accept())
            println("Client connected: ${client.socket.inetAddress.hostAddress}")
            executor.execute(ClientHandler(client, dispatcher))
        }
    }

    // Inner classes.
    class HandlerExecutor : Executor {
        override fun execute(command: Runnable?) {
            Thread(command).start()
        }
    }
}
