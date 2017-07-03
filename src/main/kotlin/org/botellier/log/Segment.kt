package org.botellier.log

import java.io.EOFException
import java.io.File
import java.nio.file.Paths

/**
 * This file contains related classes and values for manipulating segment files.
 * The format of segment files is simple, it is structured as follows:
 *
 * B0 1E 5E       Signature (3 bytes)
 * XX XX XX XX    Version of the format (4-byte int)
 * XX XX XX XX    Number of entries (4-byte int)
 *
 * All the entries come after, the specific format for each one is as follows:
 *
 * XX XX XX XX    Id (4-byte int)
 * XX XX XX XX    Marker (4-byte int)
 * XX XX XX XX    Key size (4-byte int)
 * .. .. .. ..    Key ('Key size'-byte UTF-8 encoded string)
 * .. .. .. ..
 * XX XX XX XX    Data size (4-byte int)
 * .. .. .. ..    Data ('Data size' bytes)
 * .. .. .. ..
 */

/**
 * The signature at the start of all
 * segment files.
 */
val SIGNATURE = byteArrayOf(
        0xB0.toByte(),
        0x1E.toByte(),
        0x5E.toByte()
)

/**
 * Handler for segment files.
 * @property path the base directory of the segment file.
 * @property sequence the number to use as postfix os the segment file-name.
 * @property prefix the prefix to use in the segment file-name.
 * @property maxSize the maximum size of the segment (in bytes).
 */
class Segment(val root: String, val sequence: Int, val prefix: String = "segment-", val maxSize: Int = 1*1024*1024)
    : Iterable<Entry> {
    val path = Paths.get(root, name()).toAbsolutePath().normalize()
    val file = File(path.toUri())

    /**
     * Returns the name of the segments.
     * @returns the name of the segment.
     */
    fun name() = "$prefix$sequence"

    /**
     * Returns the next segment in the sequence.
     */
    fun nextSegment() = Segment(root,sequence + 1, prefix, maxSize)

    /**
     * Clears all the content of this segment.
     */
    fun clear() = file.delete()

    /**
     * Appends a new entry to the log file indicating
     * the key and its data.
     * @param id the id to use for the entry.
     * @param key the key to use for the data.
     * @param data the bytes to use as data.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun append(id: Int, key: String, data: ByteArray) {
        if (file.length() >= maxSize) {
            throw SegmentException()
        }

        val entry = Entry(id, EntryMarker.NONE, key, data)
        file.appendBytes(entry.toByteArray())
    }

    /**
     * Appends a new entry to the log file indicating
     * the deletion of 'key'.
     * @param id the id to use for the entry.
     * @param key the key to be marked as deleted.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun delete(id: Int, key: String) {
        if (file.length() >= maxSize) {
            throw SegmentException()
        }

        val entry = Entry(id, EntryMarker.DELETE, key, byteArrayOf())
        file.appendBytes(entry.toByteArray())
    }

    // ----------------
    // Iterable
    // ----------------

    override fun iterator(): Iterator<Entry> = SegmentIterator(file)
}

class SegmentException : Throwable("Maximum log file-size reached.")

class SegmentIterator(file: File) : Iterator<Entry> {
    val inputStream = file.inputStream()
    var entry: Entry? = null

    override fun hasNext(): Boolean {
        try {
            entry = Entry.read(inputStream)
            return true
        } catch (e: EOFException) {
            return false
        }
    }

    override fun next(): Entry {
        return entry!!
    }
}
