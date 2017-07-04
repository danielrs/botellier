package org.botellier.log

import org.junit.Test
import org.junit.Assert
import java.io.File

class LogTest {
    fun log(size: Int, clear: Boolean = false, f: Log.() -> Unit) {
        val log = Log("./run", "test-segment-", size, clear)
        log.f()
        log.clear()
    }

    fun createDummy(segmentPrefix: String = "test-segment-", n: Int = 3) {
        // Delete all current files.
        val regex = Regex("^$segmentPrefix(\\d+)$")
        val folder = File("./run")
        folder.listFiles().map {
            if (it.isFile && regex.matches(it.name)) {
                it.delete()
            }
        }

        // Creates temporal files.
        for (i in 0..n-1) {
            File("./run/$segmentPrefix$i").createNewFile()
        }
    }

    @Test
    fun segmentSize() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()

        log(10) {
            set("key", before, after)
            set("key", before, after)
            set("key", before, after)
            Assert.assertTrue(File(segments[1].path.toUri()).exists())
            Assert.assertTrue(File(segments[2].path.toUri()).exists())
        }
    }

    @Test
    fun iteratingEntries() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()

        log(10) {
            set("key", before, after)
            set("key", before, after)
            set("key", before, after)

            val res = fold(Pair("", ""), { (keys, datas), entry ->
                if (entry is SetEntry) {
                    Pair(keys + entry.key, datas + String(entry.before) + String(entry.after))
                } else {
                    Pair(keys, datas)
                }
            })

            Assert.assertEquals(Pair("keykeykey", "beforeafterbeforeafterbeforeafter"), res)
        }
    }

    @Test
    fun findSegments() {
        createDummy()
        log(10) {
            Assert.assertEquals(3, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }
    }

    @Test
    fun clearSegments() {
        createDummy()
        log(10, true) {
            Assert.assertEquals(1, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }
    }
}
