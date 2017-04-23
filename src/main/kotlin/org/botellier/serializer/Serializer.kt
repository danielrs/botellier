package org.botellier.serializer

import org.botellier.store.StoreValue

interface Serializer {
    val value: StoreValue
    fun serialize(): ByteArray
}
