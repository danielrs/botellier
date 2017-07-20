package org.botellier.log

import com.google.protobuf.ByteString
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.apache.zookeeper.server.ByteBufferOutputStream
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
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

    val path: Path
    private val file: File
    private val md: MessageDigest
    private val header: SegmentHeader

    init {
        // Initialize props.
        path = Paths.get(root, name()).toAbsolutePath().normalize()
        file = File(path.toUri())
        md = MessageDigest.getInstance("MD5")
        header = if (file.exists()) SegmentHeader.parseFrom(file.inputStream()) else SegmentHeader(md)

        // Validates checksum.
        rawIterator().forEach { md.update(it) }
        val checksum = md.digest().toHexString()
        if (header.checksum != checksum) {
            throw SegmentException.ChecksumException()
        }
    }

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
     * Returns the checksum of the segment file. Note that this
     * function iterates the *whole* file, so it is going
     * to take some time with big files.
     */
    fun checksum(): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        rawIterator().forEach { md.update(it) }
        return md.digest()
    }

    // ----------------
    // Operations on segment
    // ----------------

    /**
     * Appends a new entry to the log file indicating
     * the deletion of 'key'.
     * @param id the id to use for the entry.
     * @param key the key to be marked as deleted.
     * @throws SegmentException if the file is full.
     * @see nextSegment for getting a new valid segment.
     */
    fun delete(id: Int, key: String) {
        segmentOperation(id) {
            val entry = buildDeleteEntry(id) {
                this.key = key
            }

            val buffer = ByteArrayOutputStream()
            buffer.write(entry.protos.serializedSize.toByteArray())
            buffer.write(entry.protos.toByteArray())
            buffer.toByteArray()
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
        segmentOperation(id) {
            val entry = buildSetEntry(id) {
                this.key = key
                this.before = ByteString.copyFrom(before)
                this.after = ByteString.copyFrom(after)
            }

            val buffer = ByteArrayOutputStream()
            buffer.write(entry.protos.serializedSize.toByteArray())
            buffer.write(entry.protos.toByteArray())
            buffer.toByteArray()
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
        segmentOperation(id) {
            val entry = buildSetEntry(id) {
                this.key = key
                this.before = ByteString.EMPTY
                this.after = ByteString.copyFrom(data)
            }

            val buffer = ByteArrayOutputStream()
            buffer.write(entry.protos.serializedSize.toByteArray())
            buffer.write(entry.protos.toByteArray())
            buffer.toByteArray()
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the beginning of a transaction.
     */
    fun beginTransaction(id: Int) {
        segmentOperation(id) {
            val entry = buildBeginTransactionEntry(id) {}
            val buffer = ByteArrayOutputStream()
            buffer.write(entry.protos.serializedSize.toByteArray())
            buffer.write(entry.protos.toByteArray())
            buffer.toByteArray()
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the end of a transaction.
     */
    fun endTransaction(id: Int) {
        segmentOperation(id) {
            val entry = buildEndTransactionEntry(id) {}
            val buffer = ByteArrayOutputStream()
            buffer.write(entry.protos.serializedSize.toByteArray())
            buffer.write(entry.protos.toByteArray())
            buffer.toByteArray()
        }
    }

    // ----------------
    // Misc functions.
    // ----------------

    private fun segmentOperation(id: Int, block: () -> ByteArray) {
        if (file.length() >= maxSize) {
            throw SegmentException.SizeException()
        } else {
            val data = block()

            // Updates and writes header.
            if (header.totalEntries <= 0) { header.id = id }
            md.update(data)
            header.update(md)

            val raf = RandomAccessFile(file, "rw")
            raf.seek(0)
            raf.write(header.toByteArray())

            // Writes new entry.
            file.appendBytes(data)
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

    /**
     * Iterator for raw entry data. Useful for calculating checksum of the
     * segment.
     */
    fun rawIterator(): Iterator<ByteArray> {
        if (file.exists()) {
            return RawSegmentIterator(file)
        } else {
            return Collections.emptyIterator()
        }
    }
}

sealed class SegmentException(msg: String) : Throwable(msg) {
    class SizeException : SegmentException("Maximum log-file size reached; consider using nextSegment().")
    class HeaderException : SegmentException("Invalid header")
    class ChecksumException : SegmentException("Checksum won't match data")
}

/**
 * Iterator for raw data of a segment. Useful for building higher-level iterators
 * and calculating the checksum.
 */
class RawSegmentIterator(file: File) : Iterator<ByteArray> {
    val input: DataInputStream = DataInputStream(file.inputStream().buffered())
    var next: ByteArray? = null
    init { input.skip(HEADER_SIZE.toLong()) }

    override fun hasNext(): Boolean {
        try {
            val dataSize = input.readInt()
            val data = ByteArray(dataSize)
            input.read(data)
            next = data
            return true
        } catch (e: EOFException) {
            return false
        }
    }

    override fun next(): ByteArray {
        return next!!
    }
}

/**
 * Iterator for entries in a segment file.
 * @param file the segment file to use.
 */
class SegmentIterator(file: File) : Iterator<Entry> {
    val rawIterator = RawSegmentIterator(file)
    override fun hasNext(): Boolean = rawIterator.hasNext()
    override fun next(): Entry = Entry.parseFrom(rawIterator.next())
}
