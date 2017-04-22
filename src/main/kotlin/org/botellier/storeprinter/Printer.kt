package org.botellier.storeprinter

import org.botellier.store.StoreValue

interface Printer {
    val value: StoreValue
    var pretty: Boolean
    var indent: String

    fun print(): String
}
