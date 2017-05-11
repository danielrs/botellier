package org.botellier.command

import org.botellier.store.*
import kotlin.reflect.full.createInstance

// from: https://redis.io/commands

val COMMANDS = arrayOf(
        // Lists.
        LIndexCommand::class,
        LInsertCommand::class,
        LLenCommand::class,
        LPopCommand::class,
        LPushCommand::class,
        LRangeCommand::class,
        LRemCommand::class,
        LSetCommand::class,
        LTrimCommand::class,
        // Strings.
        AppendCommand::class,
        DecrCommand::class,
        DecrbyCommand::class,
        GetCommand::class,
        IncrCommand::class,
        IncrbyCommand::class,
        IncrbyfloatCommand::class,
        MGetCommand::class,
        MSetCommand::class,
        SetCommand::class,
        StrlenCommand::class
).map { it.createInstance().name to it }.toMap()

/**
 * Lists.
 */

@WithCommand("LINDEX")
class LIndexCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var index = intValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            if (index.value < 0 || index.value > it.size - 1) {
                NilValue()
            }
            else {
                it.get(index.value)
            }
        }
    }
}

@WithCommand("LINSERT")
class LInsertCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var position = stringValue

    @field:Parameter(2)
    var pivot = anyValue

    @field:Parameter(2)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            if (position.value !in listOf("BEFORE", "AFTER")) {
                throw Command.CommandException("LINSERT BEFORE or AFTER expected.")
            }
            val before = position.value == "BEFORE"
            val value = value.toValue()
            val index = it.indexOf(pivot.toValue())
            when (index) {
                -1 -> IntValue(-1)
                else -> {
                    when {
                        before -> it.add(index, value)
                        index < it.size - 1 -> it.add(index + 1, value)
                        else -> it.rpush(value)
                    }
                    IntValue(it.size)
                }
            }
        }
    }
}

@WithCommand("LLEN")
class LLenCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        return withValue<ListValue>(store, key.value) {
            if (it != null) {
                IntValue(it.size)
            }
            else {
                IntValue(0)
            }
        }
    }
}

@WithCommand("LPOP")
class LPopCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        return withValue<ListValue>(store, key.value) {
            if (it != null && it.size > 0) {
                it.lpop()
            }
            else {
                NilValue()
            }
        }
    }
}

@WithCommand("LPUSH")
class LPushCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    @field:Parameter(2)
    var rest = anyArrayValue

    override fun execute(store: Store): StoreValue {
        return withValue<ListValue>(store, key.value) {
            val list = it ?: ListValue()
            list.lpush(value.toValue())
            rest.value.map { list.lpush(it.toValue()) }
            store.set(key.value, list)
            IntValue(list.size)
        }
    }
}

@WithCommand("LRANGE")
class LRangeCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var start = intValue

    @field:Parameter(2)
    var stop = intValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            it.slice(start.value, stop.value)
        }
    }
}

@WithCommand("LREM")
class LRemCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var count = intValue

    @field:Parameter(2)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            var count = count.value
            val indicesToRemove = mutableListOf<Int>()
            val value = value.toValue()
            when {
                count < 0 -> {
                    for (i in it.size - 1 downTo 0) {
                        if (count == 0) break
                        if (value == it.get(i)) indicesToRemove.add(i)
                        count++
                    }
                }
                count > 0 -> {
                    for (i in 0..it.size-1) {
                        if (count == 0) break
                        if (value == it.get(i)) indicesToRemove.add(i)
                        count--
                    }
                }
                count == 0 -> {
                    for (i in 0..it.size-1) {
                        if (value == it.get(i)) indicesToRemove.add(i)
                    }
                }
            }
            it.remove(indicesToRemove)
            return IntValue(indicesToRemove.size)
        }
    }
}

@WithCommand("LSET")
class LSetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var index = intValue

    @field:Parameter(2)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            if (index.value < 0 || index.value > it.size - 1) {
                throw CommandException("LSET index out of bounds")
            }
            it.set(index.value, value.toValue())
            StringValue("OK")
        }
    }
}

@WithCommand("LTRIM")
class LTrimCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var start = intValue

    @field:Parameter(2)
    var stop = intValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            it.trim(start.value, stop.value)
            if (it.size <= 0) {
                store.remove(key.value)
            }
            StringValue("OK")
        }
    }
}

/**
 * Strings.
 */

@WithCommand("APPEND")
class AppendCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        return withValue<StringValue>(store, key.value) {
            val builder = StringBuilder()
            if (it != null) {
                builder.append(it.value)
            }
            builder.append(value.toString())

            val result = builder.toString()
            store.set(key.value, result.toValue())
            result.length.toValue()
        }
    }
}

@WithCommand("DECR")
class DecrCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        val decrby = DecrbyCommand()
        decrby.key = key
        decrby.decrement = CValue.Primitive.Int(1)
        return decrby.execute(store)
    }
}

@WithCommand("DECRBY")
class DecrbyCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var decrement = intValue

    override fun execute(store: Store): StoreValue {
        val incrby = IncrbyCommand()
        incrby.key = key
        incrby.increment = CValue.Primitive.Int(-decrement.value)
        return incrby.execute(store)
    }
}

@WithCommand("GET")
class GetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue
    override fun execute(store: Store): StoreValue {
        return withPrimitive<StorePrimitive>(store, key.value) {
            it ?: NilValue()
        }
    }
}

@WithCommand("INCR")
class IncrCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        val incrby = IncrbyCommand()
        incrby.key = key
        incrby.increment = CValue.Primitive.Int(1)
        return incrby.execute(store)
    }
}

@WithCommand("INCRBY")
class IncrbyCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = intValue

    override fun execute(store: Store): StoreValue {
        return withValue<StoreNumber>(store, key.value) {
            val value = when(it) {
                is IntValue -> (it.value + increment.value).toValue()
                is FloatValue -> (it.value + increment.value.toFloat()).toValue()
                else -> increment.value.toValue()
            } as StorePrimitive
            store.set(key.value, value)
            value
        }
    }
}

@WithCommand("INCRBYFLOAT")
class IncrbyfloatCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = floatValue

    override fun execute(store: Store): StoreValue {
        return withValue<StoreNumber>(store, key.value) {
            val value = when(it) {
                is IntValue -> (it.value.toFloat() + increment.value).toValue()
                is FloatValue -> (it.value - increment.value).toValue()
                else -> increment.value.toValue()
            }
            store.set(key.value, value)
            value
        }
    }
}

@WithCommand("MGET")
class MGetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun execute(store: Store): StoreValue {
        val listValue = ListValue()

        listValue.rpush(withPrimitive<StorePrimitive>(store, key.value) { it ?: NilValue() })
        rest.value.map {
            listValue.rpush(withPrimitive<StorePrimitive>(store, it.value) { it ?: NilValue() })
        }

        return listValue
    }
}

@WithCommand("MSET")
class MSetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    @field:Parameter(2)
    var rest = pairArrayValue

    override fun execute(store: Store): StoreValue {
        store.set(key.value, value.toValue())
        for ((key, value) in rest.value) {
            store.set(key, value.toValue())
        }
        return StringValue("OK")
    }
}

@WithCommand("SET")
class SetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        store.set(key.value, value.toValue())
        return StringValue("OK")
    }
}

@WithCommand("STRLEN")
class StrlenCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        return withValue<StringValue>(store, key.value) {
            if (it != null) {
                IntValue(it.value.length)
            }
            else {
                IntValue(0)
            }
        }
    }
}

/**
 * Utility functions.
 */

/**
 * Gets 'key' from store and throws exception if 'key' doesn't exists.
 */
private inline fun <reified T, R> requireType(store: Store, key: String, body: (T) -> R): R {
    val value = store.get(key)
    if (value !is NilValue) {
        if (value is T) {
            return body(value)
        }
        else {
            throw Command.WrongTypeException(key, value.javaClass.name)
        }
    }
    else {
        throw Command.WrongTypeException(key, NilValue::class.qualifiedName ?: "NilValue")
    }
}

/**
 * Just like [requireType], except that body parameter is nullable (doesn't throw exception if 'key' lookup
 * fails).
 */
private inline fun <reified T, R> withType(store: Store, key: String, body: (T?) -> R): R {
    val value = store.get(key)
    if (value !is NilValue) {
        if (value is T) {
            return body(value)
        }
        else {
            throw Command.WrongTypeException(key, value.javaClass.name)
        }
    }
    else {
        return body(null)
    }
}

private inline fun <reified T> withValue(store: Store, key: String, body: (T?) -> StoreValue)
        = withType(store, key, body)

private inline fun <reified T> withPrimitive(store: Store, key: String, body: (T?) -> StorePrimitive)
        = withType(store, key, body)

private inline fun <reified T> requireValue(store: Store, key: String, body: (T) -> StoreValue)
        = requireType(store, key, body)
