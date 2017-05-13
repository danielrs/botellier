package org.botellier.server

import org.botellier.store.Store
import java.net.ServerSocket
import java.util.concurrent.Executor

class Server(val port: Int = 6679, val password: String? = null, dbTotal: Int = 15) {
    val dbs: List<Store> = List(dbTotal, { Store() })

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

    fun requiresPassword(): Boolean = password != null

    // Inner classes.
    class HandlerExecutor : Executor {
        override fun execute(command: Runnable?) {
            Thread(command).start()
        }
    }
}
