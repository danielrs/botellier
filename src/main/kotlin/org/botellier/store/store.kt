package org.botellier

import org.botellier.store.StoreValue
import java.util.concurrent.locks.ReentrantLock

class Store {
    var map: MutableMap<String, Pair<ReentrantLock, StoreValue>> = mutableMapOf()

    fun get(key: String): StoreValue? {
        val pair = map.get(key)
        if (pair != null) {
            val (lock, value) = pair

            lock.lock()
            val valueClone = value.clone()
            lock.unlock()

            return valueClone
        }
        else {
            return null
        }
    }

    fun set(key: String, value: StoreValue) {
        apply(key, value, { it })
    }

    fun apply(key: String, defaultValue: StoreValue, f: (StoreValue) -> StoreValue) {
        val pair = map.get(key)
        if (pair != null) {
            val (lock, value) = pair

            lock.lock()
            map.set(key, Pair(lock, f(value)))
            lock.unlock()
        }
        else {
            map.set(key, Pair(ReentrantLock(), defaultValue))
        }
    }

    override fun toString(): String = map.toString()
}