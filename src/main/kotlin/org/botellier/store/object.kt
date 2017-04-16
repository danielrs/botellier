package org.botellier.store

import org.botellier.Store

enum class ValueType {
     INT, FLOAT, STRING, LIST, SET, MAP
}

interface StoreValue {
    fun getType(): ValueType
    fun clone(): StoreValue
}

class IntValue(val initialValue: Int = 0) : StoreValue {
    var value: Int = initialValue
        set(value: Int) {
            synchronized(field, {
                field = value
            })
        }

    override fun getType(): ValueType = ValueType.INT
    override fun clone(): StoreValue = IntValue(initialValue)

    override fun toString(): String = value.toString()
}

class FloatValue(val initialValue: Double = 0.0) : StoreValue {
    var value: Double = initialValue
        set(value: Double) {
            synchronized(field, {
                field = value
            })
        }

    override fun getType(): ValueType = ValueType.FLOAT
    override fun clone(): StoreValue = FloatValue(initialValue)

    override fun toString(): String = value.toString()
}

class StringValue(val initialValue: String = "") : StoreValue {
    var value: String = initialValue
        set(value: String) {
            synchronized(field, {
                field = value
            })
        }

    override fun getType(): ValueType = ValueType.STRING
    override fun clone(): StoreValue = StringValue(initialValue)

    override fun toString(): String = value.toString()
}

class ListValue(initialValues: List<StoreValue> = mutableListOf()) : StoreValue {
    private var list: MutableList<StoreValue> = initialValues.map { it.clone() }.toMutableList()

    fun push(value: StoreValue) {
        synchronized(list, {
            list.add(value.clone())
        })
    }

    fun pop(): StoreValue {
        return synchronized(list, {
            list.removeAt(list.lastIndex)
        })
    }

    override fun getType(): ValueType = ValueType.LIST
    override fun clone(): StoreValue = ListValue(list.map { it.clone() })

    override fun toString(): String = list.joinToString(prefix = "[", postfix = "]")
}

class SetValue(initialValues: Set<String> = setOf()) : StoreValue {
    private var set: MutableSet<String> = initialValues.toMutableSet()

    fun set(key: String) {
        synchronized(set, {
            set.add(key)
        })
    }

    fun unset(key: String) {
        synchronized(set, {
            set.remove(key)
        })
    }

    fun clear() {
        synchronized(set, {
            set.clear()
        })
    }

    fun contains(key: String) {
        synchronized(set, {
            set.contains(key)
        })
    }

    override fun getType(): ValueType = ValueType.SET
    override fun clone(): StoreValue = SetValue(set.toSet())

    override fun toString(): String = set.joinToString(prefix = "{|", postfix = "|}")
}

class MapValue(initialValues: Map<String, StoreValue> = mapOf()) : StoreValue {
    private var map: MutableMap<String, StoreValue> = initialValues.mapValues { it.value.clone() }.toMutableMap()

    fun get(key: String): StoreValue? {
        return synchronized(map, {
            map.get(key)
        })
    }

    fun set(key: String, value: StoreValue) {
        synchronized(map, {
            map.set(key, value.clone())
        })
    }

    fun unset(key: String) {
        synchronized(map, {
            map.remove(key)
        })
    }

    fun clear() {
        synchronized(map, {
            map.clear()
        })
    }

    override fun getType(): ValueType = ValueType.MAP
    override fun clone(): StoreValue = MapValue(map.toMap().mapValues { it.value.clone() })

    override fun toString(): String {
        val str = StringBuilder()

        str.append("{")
        str.append(map.map { it.key + ": " + it.value.toString() }.joinToString())
        str.append("}")

        return str.toString()
    }
}

fun Int.toValue(): IntValue = IntValue(this)
fun Float.toValue(): FloatValue = FloatValue(this.toDouble())
fun Double.toValue(): FloatValue = FloatValue(this)
fun String.toValue(): StringValue = StringValue(this)
// TODO: Implement `toValue()` for lists, sets and maps.
