package org.botellier.store

import org.botellier.value.MapValue
import org.botellier.value.NilValue
import org.botellier.value.StoreValue

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
 * Store class with getter method for key/values. The only
 * way to add/modify the stored values is through a transaction.
 * @see StoreTransaction
 */
open class Store(initialMap: MapValue = MapValue()) : ReadStore, WriteStore {
    protected val map = initialMap
    override val keys get() = map.unwrap().keys
    override val size  get() = map.size

    /**
     * Gets the value of the given key.
     * @param key the key to lookup.
     * @returns the value or NilValue if key is not found.
     */
    override fun get(key: String): StoreValue {
        return map.unwrap().get(key) ?: NilValue()
    }

    /**
     * Returns a StoreTransaction instance that allows
     * modification of this store.
     * @returns the StoreTransaction instance.
     */
    override fun transaction(): StoreTransaction {
        return StoreTransaction(map)
    }
}

// ----------------
// Exceptions
// ----------------

sealed class StoreException(msg: String) : Throwable(msg) {
    class InvalidTypeException(key: String, type: String)
        : StoreException("Invalid key type: '$key' is '$type'.")
}
