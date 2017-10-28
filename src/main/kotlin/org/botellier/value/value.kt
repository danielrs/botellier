package org.botellier.value

import java.util.concurrent.ConcurrentHashMap

/**
 * Interface types, they serve the following purposes:
 *
 * - StoreType: The base type for primitives and collections.
 * - StoreWrapper: Interface for types that wrap around another type.
 * - StoreValue: It is used to represent types that can be stored
 * in a map. Maps themselves are not children of StoreValue to
 * prevent nested collections.
 * - StorePrimitive: Is either a number, string or nil; they are immutable.
 * - StoreNumber: Is a primitive that is either an int or a floating point number.
 * - StoreCollection: Can be either a list, set or map; they are mutable.
 *
 * Only stores values can be stored inside a store collection (i.e. no nested maps or list are
 * allowed).
 */

interface StoreType {
    /**
     * Clones this value (new object).
     */
    fun clone(): StoreType
}

interface StoreWrapper<out T> {
    fun unwrap(): T
}

interface StoreValue: StoreType {
    override fun clone(): StoreValue
}

interface StorePrimitive : StoreValue {
    override fun clone(): StorePrimitive
}

interface StoreNumber : StorePrimitive {
    override fun clone(): StoreNumber
}

interface StoreCollection<out T> : StoreType, Iterable<T> {
    val size: Int
    override fun clone(): StoreCollection<T>
}

// ----------------
// Primitives.
// ----------------

/**
 * Class that is inherited by all primitive types. It basically holds the field
 * for the underlying *immutable* value.
 */
abstract class PrimitiveValue<T>(initialValue: T) : StoreWrapper<T>, StorePrimitive, Comparable<PrimitiveValue<T>>
where T : Comparable<T> {
    private val value: T = initialValue

    override abstract fun clone(): PrimitiveValue<T>

    override fun unwrap() = value

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PrimitiveValue<*> -> other.value == value
            else -> other == value
        }
    }

    override fun compareTo(other: PrimitiveValue<T>): Int {
        return value.compareTo(other.value)
    }
}

class IntValue(initialValue: Int = 0) : StoreNumber, PrimitiveValue<Int>(initialValue) {
    override fun clone() = IntValue(unwrap())
    override fun toString() = unwrap().toString()
}

class FloatValue(initialValue: Double = 0.0) : StoreNumber, PrimitiveValue<Double>(initialValue) {
    override fun clone() = FloatValue(unwrap())
    override fun toString() = unwrap().toString()
}

class StringValue(initialValue: String = "") : PrimitiveValue<String>(initialValue) {
    override fun clone() = StringValue(unwrap())
    override fun toString() = unwrap()
}

class RawValue(val value: ByteArray = byteArrayOf()) : StorePrimitive {
    override fun clone(): StorePrimitive = RawValue(value.clone())
}

class NilValue : StorePrimitive {
    override fun clone() = NilValue()
    override fun toString() = "nil"
    override fun equals(other: Any?) = other == null || other is NilValue
}

// ----------------
// Collections.
// ----------------

/**
 * Wrapper over immutable List.
 */
class ListValue(private val list: List<StorePrimitive> = listOf()): StoreWrapper<List<StorePrimitive>>, StoreValue, StoreCollection<StorePrimitive> {
    /**
     * Creates a mutable clone of the immutable list and passes it to the given function. The return
     * value of that function is then used to create a new ListValue. Note that internal values
     * are not copied.
     * @param block the callback to pass the mutable list to.
     * @returns the modified ListValue.
     */
    fun copy(block: (MutableList<StorePrimitive>) -> Unit = {}): ListValue {
        val next = list.toMutableList()
        block(next)
        return ListValue(next)
    }

    override val size: Int get() = list.size
    override fun clone(): ListValue = ListValue(list)
    override fun unwrap() = list
    override fun iterator(): Iterator<StorePrimitive> = list.iterator()
    override fun toString() = list.joinToString(prefix = "[", postfix = "]")
}

// Useful extensions.

fun <T> MutableList<T>.remove(indices: List<Int>): List<T> {
    val indices = indices.sortedDescending().distinct()
    val removed = mutableListOf<T>()
    indices.map { removed.add(this.removeAt(it)) }
    return removed.toList()
}

fun <T> MutableList<T>.lpush(value: T) {
    this.add(0, value)
}

fun <T> MutableList<T>.rpush(value: T) {
    this.add(this.lastIndex + 1, value)
}

fun <T> MutableList<T>.lpop(): T {
    return this.removeAt(0)
}

fun <T> MutableList<T>.rpop(): T {
    return this.removeAt(this.lastIndex)
}

fun <T> MutableList<T>.trim(start: Int, endInclusive: Int) {
    val start = if (start < 0) (start % size + size) % size else start
    val endInclusive = if (endInclusive < 0) (endInclusive % size + size) % size else endInclusive
    if (start > endInclusive) {
        this.clear()
    } else {
        this.remove((start..endInclusive).toList())
    }
}

fun <T> List<T>.slice(start: Int, endInclusive: Int): List<T> {
    val start = if (start < 0) (start % size + size) % size else start
    val endInclusive = if (endInclusive < 0) (endInclusive % size + size) % size else endInclusive
    return this.slice(start..endInclusive)
}

/**
 * Wrapper over immutable Set.
 */
class SetValue(private val set: Set<String> = setOf()) : StoreWrapper<Set<String>>, StoreValue, StoreCollection<String> {
    /**
     * Creates a mutable clone of the immutable set and passes it to the given function. The return
     * value of that function is then used to create a new SetValue.
     * @param block the callback to pass the mutable set to.
     * @returns the modified SetValue.
     */
    fun copy(block: (MutableSet<String>) -> Unit = {}): SetValue {
        val next = set.toMutableSet()
        block(next)
        return SetValue(next)
    }

    override val size get() = set.size
    override fun clone() = SetValue(set.toSet())
    override fun unwrap() = set
    override fun iterator() = set.iterator()
    override fun toString() = set.joinToString(prefix = "[", postfix = "]")
}

/**
 * Wrapper over concurrent map.
 */
class MapValue(initialValues: Map<String, StoreValue> = mapOf()) : StoreWrapper<Map<String, StoreValue>>, StoreCollection<Map.Entry<String, StoreValue>> {
    private val map = ConcurrentHashMap(initialValues)

    /**
     * Similar to ListValue and SetValue use, except that the map passed
     * to the parameter is not a clone, it is a reference to the underlying
     * map.
     * @param block the callback to pass the map reference to.
     * @see ListValue.use
     */
    fun use(block: (MutableMap<String, StoreValue>) -> Unit) {
        block(map)
    }

    override val size get() = map.size
    override fun clone() = MapValue(map)
    override fun unwrap() = map
    override fun iterator() = map.iterator()
    override fun toString(): String {
        val str = StringBuilder()

        str.append("{")
        str.append(map.map { it.key + ": " + it.value.toString() }.joinToString())
        str.append("}")

        return str.toString()
    }
}

// ----------------
// Extension for converting built-in types to store types.
// ----------------

fun Int.toValue(): IntValue = IntValue(this)
fun Float.toValue(): FloatValue = FloatValue(this.toDouble())
fun Double.toValue(): FloatValue = FloatValue(this)
fun String.toValue(): StringValue = StringValue(this)
fun ByteArray.toValue(): RawValue = RawValue(this)
fun List<StorePrimitive>.toValue(): ListValue = ListValue(this)
fun Set<String>.toValue(): SetValue = SetValue(this)
fun Map<String, StoreValue>.toValue(): MapValue = MapValue(this)
