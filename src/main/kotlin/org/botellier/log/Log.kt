package org.botellier.log

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates a new log handler with at the specified base dir. The size
 * of each segment file can also be specified.
 * @property baseDir the base dir to use for logs.
 * @property segmentSize the size (in bytes) of each segment.
 * @property clear if set to true it will clear all existing segments and start from scratch.
 */
class Log(root: String = "./", val segmentSize: Int = 1*1024*1024, clear: Boolean = false) {
    val path: Path
    private val segments: MutableList<Segment>

    init {
        // Initializes path.
        val givenPath = Paths.get(root)
        val file = File(givenPath.toUri())

        if (file.exists() && file.isDirectory) {
            path = givenPath.toAbsolutePath().normalize()
        } else if (file.exists() && file.isFile) {
            path = givenPath.toAbsolutePath().parent.normalize()
        } else {
            path = Paths.get("./").toAbsolutePath().normalize()
        }

        // Initializes segment.
        segments = findSegments().toMutableList()

        if (clear) {
            segments.map { it.file.delete() }
        }

        if (segments.size <= 0) {
            segments.add(Segment(path.toString(), 0))
        }
    }

    /**
     * Appends a data entry to the underlying segment.
     * @see Segment
     */
    fun append(key: String, data: ByteArray) {
        try {
            segments.last().append(key, data)
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
            segments.last().delete(key)
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
                .map { println(it); it }
                .map { Pair(it[0], it[1].toInt()) }
                .sortedBy { it.second }
                .map { Segment(path.toString(), it.second) }
    }
}
