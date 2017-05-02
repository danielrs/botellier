package org.botellier.serializer

import org.botellier.store.StoreType

interface Serializer {
    val value: StoreType
    fun serialize(): ByteArray
}
