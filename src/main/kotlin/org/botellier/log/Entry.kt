package org.botellier.log

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import kotlin.system.exitProcess

private val CR = '\r'.toByte()
private val LF = '\n'.toByte()
private val CRLF = byteArrayOf(CR, LF)

data class Entry(val id: Int, val marker: EntryMarker, val key: String, val data: ByteArray) {
    companion object {
        fun read(input: InputStream): Entry {
            val dis = DataInputStream(input)

            val id = ByteArray(4)
            dis.read(id)

            val marker = ByteArray(4)
            dis.read(marker)

            val keySize = ByteArray(4)
            dis.read(keySize)

            val key = ByteArray(keySize.toInt())
            dis.read(key)

            val dataSize = ByteArray(4)
            dis.read(dataSize)

            val data = ByteArray(dataSize.toInt())
            dis.read(data)

            // Reads CR LF
            dis.readByte()
            dis.readByte()

            return Entry(id.toInt(), EntryMarker.values()[marker.toInt()], String(key), data)
        }
    }
}

fun Entry.toByteArray(): ByteArray {
    val bos = ByteArrayOutputStream()
    val dos = DataOutputStream(bos)

    dos.write(id.toByteArray())
    dos.write(marker.ordinal.toByteArray())
    dos.writeInt(key.toByteArray().size)
    dos.write(key.toByteArray())
    dos.write(data.size.toByteArray())
    dos.write(data)
    dos.write(CRLF)

    dos.close()

    return bos.toByteArray()
}

/**
 * Enum class for entry markers (tombstones, etc).
 */
enum class EntryMarker {
    NONE, DELETE
}

