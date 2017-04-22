package org.botellier.storeprinter

import org.botellier.Store

interface Printer {
    val store: Store
    var pretty: Boolean
    var indent: String

    fun print(): String
}
