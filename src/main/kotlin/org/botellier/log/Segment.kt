package org.botellier.log

import com.google.protobuf.ByteString
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * This file contains code for manipulating segment files. Manipulations such as
 * appending, deleting and clearing. The format of each segment file is simple, it
 * is composed of a series of entries; each entry begins with the size (in bytes)
 * of the entry, followed by the entry itself (protobuf encoding).
 */

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
     * Deletes the segment file.
     */
    fun clear() = file.delete()

    /**
     * Appends a new entry to the log file indicating
     * the deletion of 'key'.
     * @param id the id to use for the entry.
     * @param key the key to be marked as deleted.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun delete(id: Int, key: String) {
        ensureSize {
            val entry = buildDeleteEntry(id) {
                this.key = key
            }

            file.appendBytes(entry.protos.serializedSize.toByteArray())
            file.appendBytes(entry.protos.toByteArray())
        }
    }

    /**
     * Appends a new entry to the log file indicating
     * the key and its data.
     * @param id the id to use for the entry.
     * @param key the key to use for the data.
     * @param before the bytes to use as old data in the log.
     * @param after the bytes to use as new data in the log.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun set(id: Int, key: String, before: ByteArray, after: ByteArray) {
        ensureSize {
            val entry = buildSetEntry(id) {
                this.key = key
                this.before = ByteString.copyFrom(before)
                this.after = ByteString.copyFrom(after)
            }

            file.appendBytes(entry.protos.serializedSize.toByteArray())
            file.appendBytes(entry.protos.toByteArray())
        }
    }

    /**
     * Appends a create operation to the log. A create operation
     * has the exact same representation as a set operation, the
     * only difference is that the create operation has no data
     * for the 'before' field.
     * @param id the id to use for the entry.
     * @param key the key to use for the data.
     * @param data the bytes to use for the 'after' field.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun create(id: Int, key: String, data: ByteArray) {
        ensureSize {
            val entry = buildSetEntry(id) {
                this.key = key
                this.before = ByteString.EMPTY
                this.after = ByteString.copyFrom(data)
            }

            file.appendBytes(entry.protos.serializedSize.toByteArray())
            file.appendBytes(entry.protos.toByteArray())
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the beginning of a transaction.
     */
    fun beginTransaction(id: Int) {
        ensureSize {
            val entry = buildBeginTransactionEntry(id) {}
            file.appendBytes(entry.protos.serializedSize.toByteArray())
            file.appendBytes(entry.protos.toByteArray())
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the end of a transaction.
     */
    fun endTransaction(id: Int) {
        ensureSize {
            val entry = buildEndTransactionEntry(id) {}
            file.appendBytes(entry.protos.serializedSize.toByteArray())
            file.appendBytes(entry.protos.toByteArray())
        }
    }

    // ----------------
    // Misc functions.
    // ----------------

    private fun ensureSize(f: () -> Unit) {
        if (file.length() >= maxSize) {
            throw SegmentException()
        } else {
            f()
        }
    }

    // ----------------
    // Iterable
    // ----------------

    /**
     * Returns the iterator for each one of the entries in the segment
     * file. If the file doesn't exists, an empty iterator is returned
     * instead.
     * @returns iterator of entries.
     */
    override fun iterator(): Iterator<Entry> {
        if (file.exists()) {
            return SegmentIterator(file)
        } else {
            return Collections.emptyIterator()
        }
    }
}

class SegmentException : Throwable("Maximum log-file size reached; consider using nextSegment().")

/**
 * Iterator for entries in a segment file.
 * @param file the segment file to use.
 */
class SegmentIterator(file: File) : Iterator<Entry> {
    val inputStream = DataInputStream(file.inputStream().buffered())
    var entry: Entry? = null

    override fun hasNext(): Boolean {
        try {
            val entrySize = inputStream.readInt()
            val entryData = ByteArray(entrySize)
            inputStream.read(entryData)
            entry = Entry.parseFrom(entryData)
            return true
        } catch (e: EOFException) {
            return false
        }
    }

    override fun next(): Entry {
        return entry!!
    }
}
