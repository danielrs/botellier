package org.botellier.store

import java.util.concurrent.ConcurrentHashMap

/**
 * Interfaces.
 */

interface StoreType {
    fun clone(): StoreType
}

// StoreValue(s) can be stored on a map.
interface StoreValue : StoreType {
    override fun clone(): StoreValue
}

interface StorePrimitive : StoreValue {
    override fun clone(): StorePrimitive
}

interface StoreNumber : StorePrimitive

interface StoreCollection<out T> : StoreValue, Iterable<T> {
    val size: Int
    override fun clone(): StoreCollection<T>
}

/**
 * Primitive types.
 */

abstract class PrimitiveValue<T>(initialValue: T) : Comparable<PrimitiveValue<T>>
where T : Comparable<T> {
    var value: T = initialValue
        get() {
            synchronized(field) {
                return field
            }
        }
        set(value) {
            synchronized(field) {
                field = value
            }
        }

    override fun compareTo(other: PrimitiveValue<T>): Int {
        return value.compareTo(other.value)
    }
}

class IntValue(initialValue: Int = 0) : StoreNumber, PrimitiveValue<Int>(initialValue) {
    override fun clone(): IntValue = IntValue(value)
    override fun toString(): String = value.toString()
}

class FloatValue(initialValue: Double = 0.0) : StoreNumber, PrimitiveValue<Double>(initialValue) {
    override fun clone(): FloatValue = FloatValue(value)
    override fun toString(): String = value.toString()
}

class StringValue(initialValue: String = "") : StorePrimitive, PrimitiveValue<String>(initialValue) {
    override fun clone(): StringValue = StringValue(value)
    override fun toString(): String = value
}

/**
 * Collection types.
 */

class ListValue(initialValues: List<StorePrimitive> = mutableListOf()) : StoreCollection<StorePrimitive> {
    private var list: MutableList<StorePrimitive> = initialValues.map { it.clone() }.toMutableList()

    fun get(index: Int): StorePrimitive {
        return synchronized(list) {
            list.get(index)
        }
    }

    fun set(index: Int, value: StorePrimitive) {
        synchronized(list) {
            list.set(index, value.clone())
        }
    }

    fun remove(index: Int): StorePrimitive {
        return synchronized(list) {
            list.removeAt(index)
        }
    }

    fun lpush(value: StorePrimitive) {
        synchronized(list) {
            list.add(0, value)
        }
    }

    fun rpush(value: StorePrimitive) {
        synchronized(list) {
            list.add(value.clone())
        }
    }

    fun lpop(): StorePrimitive {
        return synchronized(list) {
            list.removeAt(0)
        }
    }

    fun rpop(): StorePrimitive {
        return synchronized(list) {
            list.removeAt(list.lastIndex)
        }
    }

    fun slice(range: IntRange): ListValue = ListValue(list.slice(range).map { it.clone() })
    fun toList(): List<StorePrimitive> = list.map { it.clone() }

    override val size get() = list.size
    override fun clone(): ListValue = ListValue(list)
    override fun iterator(): Iterator<StorePrimitive> = toList().iterator()
    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")
}

class SetValue(initialValues: Set<String> = setOf()) : StoreCollection<String> {
    private var set: MutableSet<String> = initialValues.toMutableSet()

    fun set(key: String) {
        synchronized(set) {
            set.add(key)
        }
    }

    fun unset(key: String) {
        synchronized(set) {
            set.remove(key)
        }
    }

    fun clear() {
        synchronized(set) {
            set.clear()
        }
    }

    fun contains(key: String) {
        synchronized(set) {
            set.contains(key)
        }
    }

    fun toSet(): Set<String> = set.toSet()

    override val size get() = set.size
    override fun clone(): SetValue = SetValue(set.toSet())
    override fun iterator(): Iterator<String> = toSet().iterator()
    override fun toString(): String = set.joinToString(prefix = "[", postfix = "]")
}

class MapValue(initialValues: Map<String, StoreValue> = mapOf()) : StoreType, Iterable<Map.Entry<String, StoreValue>> {
    private var map: ConcurrentHashMap<String, StoreValue> =
            ConcurrentHashMap(initialValues.mapValues { it.value.clone() }.toMap())

    val size get() = map.size

    fun get(key: String): StoreValue? {
        return map[key]
    }

    fun set(key: String, value: StoreValue) {
        map[key] = value.clone()
    }

    fun remove(key: String) {
        map.remove(key)
    }

    fun clear() {
        map.clear()
    }

    fun toMap(): Map<String, StoreValue> = map.mapValues { it.value.clone() }

    override fun clone(): MapValue = MapValue(map)
    override fun iterator(): Iterator<Map.Entry<String, StoreValue>> = toMap().iterator()
    override fun toString(): String {
        val str = StringBuilder()

        str.append("{")
        str.append(map.map { it.key + ": " + it.value.toString() }.joinToString())
        str.append("}")

        return str.toString()
    }

}

// Extension functions for common built-in types.
fun Int.toValue(): IntValue = IntValue(this)
fun Float.toValue(): FloatValue = FloatValue(this.toDouble())
fun Double.toValue(): FloatValue = FloatValue(this)
fun String.toValue(): StringValue = StringValue(this)
fun List<StorePrimitive>.toValue(): ListValue = ListValue(this)
fun Set<String>.toValue(): SetValue = SetValue(this)
fun Map<String, StoreValue>.toValue(): MapValue = MapValue(this)