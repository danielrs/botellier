package org.botellier.log

import java.io.ByteArrayOutputStream

private val CR = '\r'.toByte()
private val LF = '\n'.toByte()
private val CRLF = byteArrayOf(CR, LF)

class Entry(val marker: EntryMarker, val key: String, val data: ByteArray)

fun Entry.toByteArray(): ByteArray {
    val bos = ByteArrayOutputStream()

    bos.write(marker.ordinal)
    bos.write(CRLF)

    bos.write(key.toByteArray().size)
    bos.write(CRLF)
    bos.write(key.toByteArray())
    bos.write(CRLF)

    bos.write(data.size)
    bos.write(CRLF)
    bos.write(data)
    bos.write(CRLF)

    return bos.toByteArray()
}

/**
 * Enum class for entry markers (tombstones, etc).
 */
enum class EntryMarker {
    NONE, DELETE
}