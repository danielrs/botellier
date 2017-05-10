package org.botellier.command

import org.botellier.store.*
import kotlin.reflect.full.createInstance

// from: https://redis.io/commands

val COMMANDS = arrayOf(
        // Lists.
        LPushCommand::class,
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
            return IntValue(list.size)
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
            return result.length.toValue()
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
    override fun execute(store: Store): StoreValue = store.get(key.value)
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

//private inline fun <reified T>withType(store: Store, key: String, body: (T) -> Unit) {
//    val value = store.get(key)
//    if (value != null) {
//        if (value is T) {
//            body(value)
//        }
//        else {
//            throw Command.WrongTypeException(key, value.javaClass.name)
//        }
//    }
//    else {
//        throw Command.WrongTypeException(key, "null")
//    }
//}

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
        = withType<T, StoreValue>(store, key, body)

private inline fun <reified T> withPrimitive(store: Store, key: String, body: (T?) -> StorePrimitive)
        = withType<T, StorePrimitive>(store, key, body)
