package org.botellier.log

/**
 * Convers the given long to a byte array of 8 bytes.
 */
fun Long.toByteArray(): ByteArray {
    return byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            (this shr 0).toByte()
    )
}

/**
 * Convers the given integer to a byte array of 4 bytes.
 */
fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            (this shr 0).toByte()
    )
}

/**
 * Converts the given byte array to an integer.
 */
fun ByteArray.toInt(): Int {
    return this.take(4).fold(0, { acc, byte ->
        acc or byte.toInt()
    })
}

/**
 * Converts the given byte array to a long.
 */
fun ByteArray.toLong(): Long {
    return this.take(8).fold(0.toLong(), { acc, byte ->
        acc or byte.toLong()
    })
}