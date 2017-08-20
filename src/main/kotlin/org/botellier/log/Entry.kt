package org.botellier.log

import java.io.InputStream
import java.io.OutputStream

/**
 * Wrapper for EntryProtos.
 */
open class Entry(val protos: EntryProtos.Entry) {
    companion object {
        /**
         * Returns an Entry based on the type of EntryProtos.Entry.
         * @param protos the generated protobuf class EntryProtos.Entry.
         * @returns The wrapper for the protobuf class.
         */
        fun fromProtos(protos: EntryProtos.Entry): Entry {
            when (protos.entryTypeCase) {
                EntryProtos.Entry.EntryTypeCase.DELETE_ENTRY -> {
                    return DeleteEntry(protos)
                }
                EntryProtos.Entry.EntryTypeCase.SET_ENTRY -> {
                    return SetEntry(protos)
                }
                EntryProtos.Entry.EntryTypeCase.BEGIN_TRASACTION_ENTRY -> {
                    return BeginTransactionEntry(protos)
                }
                EntryProtos.Entry.EntryTypeCase.END_TRANSACTION_ENTRY -> {
                    return EndTransactionEntry(protos)
                }
                else -> {
                    return Entry(protos)
                }
            }
        }

        // Parsing.
        fun parseFrom(input: InputStream): Entry {
            return fromProtos(EntryProtos.Entry.parseFrom(input))
        }

        fun parseFrom(input: ByteArray): Entry {
            return fromProtos(EntryProtos.Entry.parseFrom(input))
        }
    }

    val id: Int get() = protos.id

    fun writeTo(output: OutputStream) {
        protos.writeTo(output)
    }

    // Overloads.
    override fun toString(): String = protos.toString()
}

// ----------------
// Entry types.
// ----------------

class DeleteEntry(protos: EntryProtos.Entry) : Entry(protos) {
    val key: String get() = protos.deleteEntry.key
}

class SetEntry(protos: EntryProtos.Entry) : Entry(protos) {
    val key: String get() = protos.setEntry.key
    val before: ByteArray by lazy { protos.setEntry.before.toByteArray() }
    val after: ByteArray by lazy { protos.setEntry.after.toByteArray() }
}

class BeginTransactionEntry(protos: EntryProtos.Entry) : Entry(protos)

class EndTransactionEntry(protos: EntryProtos.Entry) : Entry(protos)

// ----------------
// Builders.
// ----------------

fun buildDeleteEntry(id: Int, init: EntryProtos.DeleteEntry.Builder.() -> Unit): Entry {
    val entry = EntryProtos.Entry.newBuilder()
    val deleteEntry = EntryProtos.DeleteEntry.newBuilder()

    deleteEntry.init()
    entry.id = id
    entry.deleteEntry = deleteEntry.build()

    return Entry.fromProtos(entry.build())
}

fun buildSetEntry(id: Int, init: EntryProtos.SetEntry.Builder.() -> Unit): Entry {
    val entry = EntryProtos.Entry.newBuilder()
    val setEntry = EntryProtos.SetEntry.newBuilder()

    setEntry.init()
    entry.id = id
    entry.setEntry = setEntry.build()

    return Entry.fromProtos(entry.build())
}

fun buildBeginTransactionEntry(id: Int, init: EntryProtos.BeginTransactionEntry.Builder.() -> Unit = {}): Entry {
    val entry = EntryProtos.Entry.newBuilder()
    val transactionEntry = EntryProtos.BeginTransactionEntry.newBuilder()

    transactionEntry.init()
    entry.id = id
    entry.beginTrasactionEntry = transactionEntry.build()

    return Entry.fromProtos(entry.build())
}

fun buildEndTransactionEntry(id: Int, init: EntryProtos.EndTransactionEntry.Builder.() -> Unit = {}): Entry {
    val entry = EntryProtos.Entry.newBuilder()
    val transactionEntry = EntryProtos.EndTransactionEntry.newBuilder()

    transactionEntry.init()
    entry.id = id
    entry.endTransactionEntry = transactionEntry.build()

    return Entry.fromProtos(entry.build())
}
