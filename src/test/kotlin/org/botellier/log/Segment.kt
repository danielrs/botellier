package org.botellier.log

import org.junit.Test
import org.junit.Assert

class SegmentTest {
    fun segment(maxSize: Int = 1*1024*1024, f: (Segment) -> Unit) {
        val s = Segment("./run", 0, "test-tmp-segment-", maxSize)
        try {
            f(s)
        } catch (e: Throwable) {
            throw e
        } finally {
            s.clear()
        }
    }

    @Test(expected = SegmentException::class)
    fun maxSize() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()
        segment(24) {
            it.set(0, "none", before, after)
            it.set(0, "none", before, after)
        }
    }

    @Test
    fun iterating() {
        segment(200) {
            val oneBytes = "one".toByteArray()
            val twoBytes = "two".toByteArray()
            val threeBytes = "three".toByteArray()

            it.set(0, "one", oneBytes, oneBytes)
            it.delete(1, "one")
            it.set(2, "two", twoBytes, twoBytes)
            it.delete(3, "two")
            it.set(4, "three", threeBytes, threeBytes)
            it.delete(5, "three")

            val res = it.fold(Pair(0, ""), { (sum, str), entry ->
                var key = ""

                if (entry is DeleteEntry) {
                    key = entry.key
                } else if (entry is SetEntry) {
                    key = entry.key
                }

                Pair(sum + entry.id, str + key)
            })

            Assert.assertEquals(Pair(15, "oneonetwotwothreethree"), res)
        }
    }

    @Test
    fun createAndSetEntries() {
        segment(200) {
            it.create(0, "zero", "0".toByteArray())
            it.create(1, "one", "1".toByteArray())
            it.set(2, "zero", "0".toByteArray(), "zero".toByteArray())
            it.set(3, "one", "1".toByteArray(), "one".toByteArray())

            val res = it.fold("", { str, entry ->
                if (entry is SetEntry) {
                    str + String(entry.before) + String(entry.after)
                } else {
                    str
                }
            })

            Assert.assertEquals("010zero1one", res)
        }
    }
}
