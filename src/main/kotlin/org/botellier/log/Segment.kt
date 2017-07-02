package org.botellier.log

import java.io.File
import java.nio.file.Paths

/**
 * Handler for segment files.
 * @property path the base directory of the segment file.
 * @property sequence the number to use as postfix os the segment file-name.
 * @property prefix the prefix to use in the segment file-name.
 * @property maxSize the maximum size of the segment (in bytes).
 */
class Segment(val root: String, val sequence: Int, val prefix: String = "segment-", val maxSize: Long = 1*1024*1024) {
    val path = Paths.get(root, name()).toAbsolutePath().normalize()
    private val file = File(path.toUri())

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
     * @param key the key to use for the data.
     * @param data the bytes to use as data.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun append(key: String, data: ByteArray) {
        if (file.length() >= maxSize) {
            throw SegmentException()
        }

        val entry = Entry(EntryMarker.NONE, key, data)
        file.appendBytes(entry.toByteArray())
    }


    /**
     * Appends a new entry to the log file indicating
     * the deletion of 'key'.
     * @param key the key to be marked as deleted.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun delete(key: String) {
        if (file.length() >= maxSize) {
            throw SegmentException()
        }

        val entry = Entry(EntryMarker.DELETE, key, byteArrayOf())
        file.appendBytes(entry.toByteArray())
    }
}

class SegmentException : Throwable("Maximum log file-size reached.")
