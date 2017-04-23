package org.botellier.storeprinter

import org.botellier.store.StoreValue

interface Printer {
    val value: StoreValue

    fun print(): String
}
