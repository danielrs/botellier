package org.botellier.log

import org.junit.Test
import org.junit.Assert
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

class SegmentTest {
    fun segment(maxSize: Int = 1*1024*1024, f: (Segment) -> Unit) {
        withDummy {
            val s = Segment(it.toString(), 0, "test-tmp-segment-", maxSize)
            try {
                f(s)
            } catch (e: Throwable) {
                throw e
            } finally {
                s.clear()
            }
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
        segment(250) {
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
    fun iteratingEmpty() {
        segment {
            val res = it.fold(0, { sum, entry ->
                sum + entry.id
            })
            Assert.assertEquals(0, res)
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

    @Test
    fun checksumValidation() {
        withDummy {
            val segment = Segment(it.toString(), 0, "test-segment-")
            segment.create(0, "zero", "0".toByteArray())
            segment.create(1, "one", "1".toByteArray())
            segment.create(2, "two", "2".toByteArray())

            Segment(it.toString(), 0, "test-segment-")
        }
    }

    @Test
    fun totalEntries() {
        segment {
            Assert.assertEquals(0, it.totalEntries)
            it.create(0, "one", "1".toByteArray())
            it.create(1, "two", "2".toByteArray())
            it.create(2, "three", "3".toByteArray())
            Assert.assertEquals(3, it.totalEntries)
            it.beginTransaction(3)
            it.set(4, "one", "1".toByteArray(), "one".toByteArray())
            it.endTransaction(5)
            Assert.assertEquals(6, it.totalEntries)
        }
    }

    @Test
    fun segmentId() {
        segment {
            Assert.assertEquals(-1, it.id)
            it.create(99, "one", "1".toByteArray())
            Assert.assertEquals(99, it.id)
            it.create(100, "two", "2".toByteArray())
            Assert.assertEquals(99, it.id)
        }
    }

    @Test
    fun messageDigest() {
        val md0 = MessageDigest.getInstance("MD5")
        val md1 = MessageDigest.getInstance("MD5")

        md0.update(byteArrayOf(0, 1, 2, 3))

        md1.update(byteArrayOf(0, 1))
        md1.update(byteArrayOf(2))
        md1.update(byteArrayOf(3))

        Assert.assertArrayEquals(md0.digest(), md1.digest())
    }

    @Test (expected = Throwable::class)
    fun corruptedHeader() {
        withDummy {
            // Create segment.
            val segment0 = Segment(it.toString(), 0, "test-segment-")
            segment0.create(0, "one", "1".toByteArray())

            // Writes invalid data to header.
            val file = File(segment0.path.toUri())
            val raf = RandomAccessFile(file, "rw")
            raf.seek(0)
            raf.write(byteArrayOf(0, 0, 0, 0))

            // Tries to read segment again.
            val segment1 = Segment(it.toString(), 0, "test-segment-")
        }
    }
}
