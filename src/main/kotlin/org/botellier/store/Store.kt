package org.botellier.store

import org.botellier.log.Log

/**
 * Read only store.
 */
interface ReadStore {
    val keys: Set<String>
    val size: Int
    fun get(key: String): StoreValue
}

/**
 * Write store.
 */
interface WriteStore {
    fun transaction(): StoreTransaction
}

/**
 * Wrapper class that returns all changes made to map in a wrapper "transaction"
 * class 'StoreChange'. None of the changes made are applied unless the user
 * "commits" the store change returned by each one of the methods.
 */
class Store(initialMap: MapValue = MapValue()) : ReadStore, WriteStore {
    private val store = initialMap
    val log = Log()

    override val keys get() = store.map.keys
    override val size  get() = store.size

    override fun get(key: String): StoreValue {
        return store.map.get(key) ?: NilValue()
    }

    override fun transaction(): StoreTransaction {
        return StoreTransaction(store)
    }
}

// ----------------
// Exceptions
// ----------------

sealed class StoreException(msg: String) : Throwable(msg) {
    class InvalidTypeException(key: String, type: String)
        : StoreException("Invalid key type: '$key' is '$type'.")
}
