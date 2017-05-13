package org.botellier

import org.botellier.server.Server

fun main(args: Array<String>) {
    val server = Server(password = "password")
    server.start()
}