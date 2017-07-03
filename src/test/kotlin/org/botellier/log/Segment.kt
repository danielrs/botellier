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
        segment(24) {
            it.append(0, "None", "None".toByteArray())
            it.append(0, "None", "None".toByteArray())
        }
    }

    @Test
    fun appendingAndIterating() {
        val entries = listOf<Entry>(
                Entry(0, EntryMarker.NONE, "One", "One".toByteArray()),
                Entry(1, EntryMarker.DELETE, "One", byteArrayOf()),
                Entry(2, EntryMarker.NONE, "Two", "Two".toByteArray()),
                Entry(3, EntryMarker.DELETE, "Two", byteArrayOf()),
                Entry(4, EntryMarker.NONE, "Three", "Three".toByteArray()),
                Entry(5, EntryMarker.DELETE, "Three", byteArrayOf())
        )

        segment(200) {
            it.append(0, "One", "One".toByteArray())
            it.delete(1, "One")
            it.append(2, "Two", "Two".toByteArray())
            it.delete(3, "Two")
            it.append(4, "Three", "Three".toByteArray())
            it.delete(5, "Three")

            for (entry in it) {
                println(entry)
//                Assert.assertEquals(entries[entry.id].id, entry.id)
//                Assert.assertEquals(entries[entry.id].marker, entry.marker)
//                Assert.assertEquals(entries[entry.id].key, entry.key)
//                Assert.assertArrayEquals(entries[entry.id].data, entry.data)
            }
        }
    }
}
