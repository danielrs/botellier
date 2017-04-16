package org.botellier

import org.botellier.store.StoreValue
import java.util.concurrent.locks.ReentrantLock

class Store {
    var hashMap: HashMap<String, Pair<ReentrantLock, StoreValue>> = HashMap()

    fun get(key: String): StoreValue? {
        val pair = hashMap.get(key)
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
        val pair = hashMap.get(key)
        if (pair != null) {
            val (lock, value) = pair

            lock.lock()
            hashMap.set(key, Pair(lock, f(value)))
            lock.unlock()
        }
        else {
            hashMap.set(key, Pair(ReentrantLock(), defaultValue))
        }
    }
}