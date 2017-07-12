package org.botellier.serializer

import org.botellier.value.StoreType

interface Serializer {
    val value: StoreType?
    fun serialize(): ByteArray
}
