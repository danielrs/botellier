package org.botellier.log

import org.junit.Assert
import org.junit.Test

class EntryTest {
    val CR = '\r'.toByte()
    val LF = '\n'.toByte()

    val entry = Entry(0, EntryMarker.NONE, "None", "None".toByteArray())
    val entryBytes = byteArrayOf(
             0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 4,
            78, 111, 110, 101,
            0, 0, 0, 4,
            78, 111, 110, 101,
            CR, LF
    )

    @Test
    fun serializeEntry() {
        Assert.assertArrayEquals(entryBytes, entry.toByteArray())
    }

    @Test
    fun readInputStream() {
        val parsedEntry = Entry.read(entryBytes.inputStream())
        Assert.assertEquals(entry.id, parsedEntry.id)
        Assert.assertEquals(entry.marker, parsedEntry.marker)
        Assert.assertEquals(entry.key, parsedEntry.key)
        Assert.assertArrayEquals(entry.data, parsedEntry.data)
    }
}
