package org.botellier.server

import java.net.Socket

class Reply(val client: Socket, val bytes: ByteArray) {
    fun send() {
        TODO("REPLY")
    }
}
