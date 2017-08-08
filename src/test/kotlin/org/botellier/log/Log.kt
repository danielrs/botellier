package org.botellier.log

import org.junit.Test
import org.junit.Assert
import java.io.File
import java.util.*

class LogTest {
    fun log(root: String, segmentSize: Int, clear: Boolean = false, f: Log.() -> Unit) {
        val log = Log(root, "test-segment-", segmentSize, clear)
        log.f()
        log.clear()
    }

    @Test
    fun segmentSize() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()
        withDummy { log(it.toString(), 10) {
            set("key", before, after)
            set("key", before, after)
            set("key", before, after)
            Assert.assertTrue(File(segments[1].path.toUri()).exists())
            Assert.assertTrue(File(segments[2].path.toUri()).exists())
        }}
    }

    @Test
    fun iteratingEntries() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()

        withDummy { log(it.toString(),10) {
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
        }}
    }

    @Test
    fun findSegments() {
        withDummy(3) { log(it.toString(), 10) {
            Assert.assertEquals(3, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }}
    }

    @Test
    fun clearSegments() {
        withDummy(3) { log(it.toString(), 10, true) {
            Assert.assertEquals(1, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }}
    }

    @Test
    fun skippingEntriesOnEmptyLog() {
        withDummy { log(it.toString(), 10, true) {
            val res = query(87)
            Assert.assertEquals(0, res.toList().size)
        }}
    }

    @Test
    fun skippingEntriesOnSingleSegmentLog() {
        withDummy { log(it.toString(), 1024*1024, true) {
            for (i in 0..100) {
                create("$i", "$i".toByteArray())
            }

            val res = query(87).fold("", { acc, entry ->
                acc + entry.id
            })

            Assert.assertEquals(1, segments.size)
            Assert.assertEquals("87888990919293949596979899100", res)
        }}
    }

    @Test
    fun skippingEntriesOnMultipleSegmentLog() {
        withDummy { log(it.toString(), 10, true) {
            for (i in 0..100) {
                create("$i", "$i".toByteArray())
            }

            val res = query(95).fold("", { acc, entry ->
                acc + entry.id
            })

            Assert.assertEquals(101, segments.size)
            Assert.assertEquals("9596979899100", res)
        }}
    }

    @Test
    fun extendingWithValidSequence() {
        withDummy {
            val baseLog = Log(it.toString(), "test-segment-base-")
            baseLog.create("zero", "0".toByteArray())
            baseLog.create("one", "1".toByteArray())
            baseLog.create("two", "2".toByteArray())
            baseLog.create("three", "3".toByteArray())
            baseLog.create("four", "4".toByteArray())

            val extendedLog = Log(it.toString(), "test-segment-ext-")
            extendedLog.create("zero", "0".toByteArray())
            extendedLog.extend(baseLog.query(1))

            val baseRes = baseLog.fold("", { acc, entry ->
                acc + (entry as SetEntry).key
            })

            val extendedRes = extendedLog.fold("", { acc, entry ->
                acc + (entry as SetEntry).key
            })

            Assert.assertEquals(baseRes, extendedRes)
        }
    }

    @Test(expected = LogException.ExtendException::class)
    fun extendingWithInvalidSequence() {
        withDummy {
            val baseLog = Log(it.toString(), "test-segment-base-")
            baseLog.create("zero", "0".toByteArray())
            baseLog.create("one", "1".toByteArray())
            baseLog.create("two", "2".toByteArray())
            baseLog.create("three", "3".toByteArray())
            baseLog.create("four", "4".toByteArray())

            val extendedLog = Log(it.toString(), "test-segment-ext-")
            extendedLog.create("zero", "0".toByteArray())
            extendedLog.extend(baseLog.query(2))
        }

    }
}
