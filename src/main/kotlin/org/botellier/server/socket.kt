package org.botellier.server

import java.io.BufferedInputStream
import java.net.Socket

/**
 * Waits for the InputStream until ready by given timeout. A timeout of '0' indicates to wait indefinitely.
 * @param timeout the time to wait for input before continuing.
 * @return BufferedInputStream the input stream ready to be read.
 */
fun Socket.waitInput(timeout: Int = 0): BufferedInputStream {
    val prevTimeout = this.soTimeout
    val stream = this.getInputStream().buffered()

    this.soTimeout = timeout
    stream.mark(1)
    stream.read()
    stream.reset()
    this.soTimeout = prevTimeout

    return stream
}
