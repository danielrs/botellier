package org.botellier.log

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates a new log handler with at the specified base dir. The size
 * of each segment file can also be specified.
 * @param root the base dir to use for logs.
 * @param clear if set to true, it will clear all existing segments and start from scratch.
 * @property segmentSize the size (in bytes) of each segment.
 * @property path The base directory that holds all the logs.
 * @property id an monotonic increasing value that changes each time an entry is added.
 */
class Log(root: String = "./", val segmentPrefix: String = "segment-", val segmentSize: Int = 2*1024*1024, clear: Boolean = false)
    : Iterable<Entry> {
    val path: Path
    var id: Int private set

    val segments: MutableList<Segment>

    init {
        // Initializes path.
        val givenPath = Paths.get(root)
        val file = File(givenPath.toUri())

        if (file.isDirectory) {
            path = givenPath.toAbsolutePath().normalize()
        } else if (file.isFile) {
            path = givenPath.toAbsolutePath().parent.normalize()
        } else {
            file.mkdirs()
            path = givenPath.toAbsolutePath().normalize()
        }

        // Initializes segment.
        segments = findSegments().toMutableList()
        id = findId(segments)

        if (clear) { this.clear() }
        if (segments.size <= 0) {
            segments.add(Segment(path.toString(), 0, segmentPrefix, segmentSize))
        }
    }

    /**
     * Clears the segment files for this log.
     */
    fun clear() {
        segments.map { it.clear() }
        segments.clear()
    }

    /**
     * Finds existing segments in the given path.
     */
    fun findSegments(): List<Segment> {
        val regex = Regex("^($segmentPrefix)(\\d+)$")
        val folder = File(path.toUri())
        return folder.listFiles()
                .filter { it.isFile && regex.matches(it.name) }
                .map { regex.find(it.name)!! }
                .map { Pair(it.groupValues[1], it.groupValues[2].toInt()) }
                .sortedBy { it.second }
                .map { Segment(path.toString(), it.second, segmentPrefix, segmentSize) }
    }

    /**
     * Finds the best id value based on the given segments.
     */
    private fun findId(segments: List<Segment>): Int {
        if (segments.isEmpty()) {
            return 0
        } else {
            return 0
        }
    }

    // ----------------
    // Entry operations.
    // ----------------

    private fun segmentOperation(f: (Int, Segment) -> Unit) {
        try {
            f(id, segments.last())
            id++
        } catch (e: SegmentException) {
            segments.add(segments.last().nextSegment())
            segmentOperation(f)
        }
    }

    /**
     * Appends a deletion entry to the underlying segment.
     * @see Segment.delete
     */
    fun delete(key: String) {
        segmentOperation { id, segment ->
            segment.delete(id, key)
        }
    }

    /**
     * Appends a data entry to the underlying segment.
     * @see Segment.set
     */
    fun set(key: String, before: ByteArray, after: ByteArray) {
        segmentOperation { id, segment ->
            segment.set(id, key, before, after)
        }
    }

    /**
     * Appends a data entry to the underlying segment.
     * @see Segment.create
     */
    fun create(key: String, data: ByteArray) {
        segmentOperation { id, segment ->
            segment.create(id, key, data)
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the beginning of a transaction.
     * @see Segment.beginTransaction
     */
    fun beginTransaction(id: Int) {
        segmentOperation { id, segment ->
            segment.beginTransaction(id)
        }
    }

    /**
     * Appends a new entry to the log indicating
     * the end of a transaction.
     * @see Segment.endTransaction
     */
    fun endTransaction(id: Int) {
        segmentOperation { id, segment ->
            segment.endTransaction(id)
        }
    }

    // ----------------
    // Iterable.
    // ----------------

    override fun iterator(): Iterator<Entry> = LogIterator(segments)
}

class LogIterator(segments: List<Segment>) : Iterator<Entry> {
    var iterators = segments.map { it.iterator() }

    override fun hasNext(): Boolean {
        if (iterators.isNotEmpty() && iterators.first().hasNext()) {
            return true
        } else if (iterators.isNotEmpty()) {
            iterators = iterators.drop(1)
            return hasNext()
        } else {
            return false
        }
    }

    override fun next(): Entry {
        return iterators.first().next()
    }
}
