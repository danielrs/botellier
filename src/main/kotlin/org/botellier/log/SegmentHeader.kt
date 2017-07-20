package org.botellier.log

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest


const val HEADER_SIZE = 44

/**
 * Class with data that should be present at the beginning of each segment file. It contains
 * important information such as the id of the first entry (useful for lookups) and the
 * checksum of the segment (useful for integrity validation).
 *
 * The size of the serialized header is always 28 bytes. As the first 4 bytes are used
 * for specifying the size of the underlying protocol buffer (which can be variable size);
 * the rest of bytes are padded to meet the 28 bytes.
 */
class SegmentHeader private constructor(initialId: Int, initialChecksum: String, initialEntries: Int) {
    companion object {
        fun parseFrom(input: InputStream): SegmentHeader {
            try {
                val protos = SegmentHeaderProtos.SegmentHeader.parseDelimitedFrom(input)
                protos ?: throw EOFException()
                return SegmentHeader(protos.id, protos.checksum, protos.totalEntries)
            } catch (e: Throwable) {
                return SegmentHeader(MessageDigest.getInstance("MD5"))
            }
        }

        fun parseFrom(data: ByteArray): SegmentHeader {
            return parseFrom(data.inputStream())
        }
    }

    var id = initialId
    var checksum = initialChecksum; private set
    var totalEntries = initialEntries; private set

    /**
     * Creates a new SegmentHeader.
     * @param md the MessageDigest to use for initial checksum.
     */
    constructor(md: MessageDigest) : this(0, "", 0) {
        checksum = md.digest().toHexString()
    }

    /**
     * Updates the checksum and number of entries indicated in
     * the header.
     * @param md the MessageDigest instance to use for obtaining the checksum.
     */
    fun update(md: MessageDigest) {
        checksum = md.digest().toHexString()
        totalEntries++
    }

    /**
     * Converts this instance to a SegmentHeaderProtos.SegmentHeader,
     * which can be easily serialized.
     */
    fun toProtos(): SegmentHeaderProtos.SegmentHeader {
        val builder = SegmentHeaderProtos.SegmentHeader.newBuilder()
        builder.id = id
        builder.checksum = checksum
        builder.totalEntries = totalEntries
        return builder.build()
    }

    /**
     * Writes the padded protocol buffer to the beginning of the
     * given output stream.
     */
    fun writeTo(output: OutputStream) {
        output.write(toByteArray())
    }

    /**
     * Serializes the SegmentHeader adding the required padding.
     * @returns the padded bytes of the segment header data.
     */
    fun toByteArray(): ByteArray {
        val protos = toProtos()
        val buffer = ByteArrayOutputStream(HEADER_SIZE)
        protos.writeDelimitedTo(buffer)
        val paddingSize = HEADER_SIZE - buffer.size()
        buffer.write(ByteArray(paddingSize, { 0 }))
        return buffer.toByteArray()
    }
}

// ----------------
// Useful extensions.
// ----------------

private val hexDigits = setOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b' ,'c' ,'d', 'e', 'f',
        'A', 'B' ,'C' ,'D', 'E', 'F'
)

/**
 * Converts the contained bytes to
 * an hexadecimal string.
 */
fun ByteArray.toHexString(): String {
    val buffer = StringBuffer()
    for (b in this) {
        val hex = Integer.toHexString(b.toInt() and 0xff)
        if (hex.length < 2) { buffer.append('0') }
        buffer.append(hex)
    }
    return buffer.toString()
}