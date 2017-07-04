package org.botellier.log

import com.google.protobuf.ByteString
import org.junit.Assert
import org.junit.Test

class EntryTest {
    @Test
    fun buildingDeleteEntry() {
        val entry = buildDeleteEntry(0) {
            this.key = "key"
        }
        Assert.assertTrue(entry is DeleteEntry)
    }

    @Test
    fun buildingSetEntry() {
        val entry = buildSetEntry(0) {
            this.key = "key"
            this.before = ByteString.copyFrom(byteArrayOf(48, 49, 50))
            this.after = ByteString.copyFrom(byteArrayOf(51, 52, 53))
        }
        Assert.assertTrue(entry is SetEntry)
    }

    @Test
    fun buildingBeginTransactionEntry() {
        val entry = buildBeginTransactionEntry(0) {}
        Assert.assertTrue(entry is BeginTransactionEntry)
    }

    @Test
    fun buildingEndTransactionEntry() {
        val entry = buildEndTransactionEntry(0) {}
        Assert.assertTrue(entry is EndTransactionEntry)
    }
}
