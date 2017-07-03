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
class Log(root: String = "./", val segmentSize: Int = 2*1024*1024, clear: Boolean = false)
    : Iterable<Entry> {
    val path: Path
    var id: Long private set

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
            path = Paths.get("./").toAbsolutePath().normalize()
        }

        // Initializes segment.
        segments = findSegments().toMutableList()
        id = findId(segments)

        if (clear) {
            segments.map { it.clear() }
            segments.clear()
        }

        if (segments.size <= 0) {
            segments.add(Segment(path.toString(), 0, maxSize = segmentSize))
        }
    }

    /**
     * Appends a data entry to the underlying segment.
     * @see Segment
     */
    fun append(key: String, data: ByteArray) {
        try {
            segments.last().append(id.toInt(), key, data)
            id++
        } catch(e: SegmentException) {
            segments.add(segments.last().nextSegment())
            append(key, data)
        }
    }

    /**
     * Appends a deletion entry to the underlying segment.
     * @see Segment
     */
    fun delete(key: String) {
        try {
            segments.last().delete(id.toInt(), key)
            id++
        } catch(e: SegmentException) {
            segments.add(segments.last().nextSegment())
            delete(key)
        }
    }

    /**
     * Finds existing segments in the given path.
     */
    private fun findSegments(): List<Segment> {
        val regex = Regex("^segment-(\\d+)$")
        val folder = File(path.toUri())
        return folder.listFiles()
                .filter { it.isFile && regex.matches(it.name) }
                .map { it.name.split("-") }
                .map { Pair(it[0], it[1].toInt()) }
                .sortedBy { it.second }
                .map { Segment(path.toString(), it.second) }
    }

    /**
     * Finds the best id value based on the given segments.
     */
    private fun findId(segments: List<Segment>): Long {
        if (segments.isEmpty()) {
            return 0
        } else {
            return 0
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
