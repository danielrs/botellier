package org.botellier.store

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

enum class ValueType {
     INT, FLOAT, STRING, LIST, SET, MAP
}

interface StoreValue : Serializable {
    abstract val type: ValueType
    fun clone(): StoreValue
}

abstract class StorePrimitive<T>(initialValue: T) : StoreValue, Comparable<StorePrimitive<T>>
    where T: Comparable<T> {
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

    override fun compareTo(other: StorePrimitive<T>): Int {
        return value.compareTo(other.value)
    }
}

interface StoreCollection<out T> : StoreValue, Iterable<T> {
    val size: Int
}

class IntValue(initialValue: Int = 0) : StorePrimitive<Int>(initialValue) {
    override val type: ValueType = ValueType.INT
    override fun clone(): IntValue = IntValue(value)
    override fun toString(): String = value.toString()
}

class FloatValue(initialValue: Double = 0.0) : StorePrimitive<Double>(initialValue) {
    override val type: ValueType = ValueType.FLOAT
    override fun clone(): FloatValue = FloatValue(value)
    override fun toString(): String = value.toString()
}

class StringValue(initialValue: String = "") : StorePrimitive<String>(initialValue) {
    override val type: ValueType = ValueType.STRING
    override fun clone(): StringValue = StringValue(value)
    override fun toString(): String = value
}

class ListValue(initialValues: List<StoreValue> = mutableListOf()) : StoreCollection<StoreValue> {
    private var list: MutableList<StoreValue> = initialValues.map { it.clone() }.toMutableList()

    fun get(index: Int): StoreValue {
        return synchronized(list) {
            list.get(index)
        }
    }

    fun set(index: Int, value: StoreValue) {
        synchronized(list) {
            list.set(index, value.clone())
        }
    }

    fun remove(index: Int): StoreValue {
        return synchronized(list) {
            list.removeAt(index)
        }
    }

    fun lpush(value: StoreValue) {
        synchronized(list) {
            list.add(0, value)
        }
    }

    fun rpush(value: StoreValue) {
        synchronized(list) {
            list.add(value.clone())
        }
    }

    fun lpop(): StoreValue {
        return synchronized(list) {
            list.removeAt(0)
        }
    }

    fun rpop(): StoreValue {
        return synchronized(list) {
            list.removeAt(list.lastIndex)
        }
    }

    fun slice(range: IntRange): ListValue = ListValue(list.slice(range).map { it.clone() })
    fun toList(): List<StoreValue> = list.map{ it.clone() }

    override val type: ValueType = ValueType.LIST
    override fun clone(): ListValue = ListValue(list)

    override val size get() = list.size
    override fun iterator(): Iterator<StoreValue> = toList().iterator()

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

    override val type: ValueType = ValueType.SET
    override fun clone(): SetValue = SetValue(set.toSet())

    override val size get() = set.size
    override fun iterator(): Iterator<String> = toSet().iterator()

    override fun toString(): String = set.joinToString(prefix = "[", postfix = "]")
}

class MapValue(initialValues: Map<String, StoreValue> = mapOf()) : StoreCollection<Map.Entry<String, StoreValue>> {
    private var map: ConcurrentHashMap<String, StoreValue> =
            ConcurrentHashMap(initialValues.mapValues { it.value.clone() }.toMap())

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

    override val type: ValueType = ValueType.MAP
    override fun clone(): MapValue = MapValue(map)

    override val size get() = map.size
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
fun List<StoreValue>.toValue(): ListValue = ListValue(this)
fun Set<String>.toValue(): SetValue = SetValue(this)
fun Map<String, StoreValue>.toValue(): MapValue = MapValue(this)
