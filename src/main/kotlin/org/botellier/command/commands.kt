package org.botellier.command

import org.botellier.store.*
import kotlin.reflect.full.createInstance

// from: https://redis.io/commands

val COMMANDS = arrayOf(
        AppendCommand::class,
        DecrCommand::class,
        DecrbyCommand::class,
        GetCommand::class,
        IncrCommand::class,
        IncrbyCommand::class,
        IncrbyfloatCommand::class,
        SetCommand::class,
        StrlenCommand::class
).map { it.createInstance().name to it }.toMap()

/**
 * Strings
 */

@WithCommand("APPEND")
class AppendCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun execute(store: Store): StoreValue? {
        return withType<StringValue>(store, key.value) {
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

    override fun execute(store: Store): StoreValue? {
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

    override fun execute(store: Store): StoreValue? {
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
    override fun execute(store: Store): StoreValue? = store.get(key.value)
}

@WithCommand("INCR")
class IncrCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue? {
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

    override fun execute(store: Store): StoreValue? {
        return withType<StoreNumber>(store, key.value) {
            if (it == null) {
                store.set(key.value, increment.value.toValue())
            }
            else {
                when (it) {
                    is IntValue ->
                        store.set(key.value, (it.value + increment.value).toValue())
                    is FloatValue ->
                        store.set(key.value, (it.value + increment.value.toFloat()).toValue())
                }
            }
            store.get(key.value)
        }
    }
}

@WithCommand("INCRBYFLOAT")
class IncrbyfloatCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = floatValue

    override fun execute(store: Store): StoreValue? {
        return withType<StoreNumber>(store, key.value) {
            if (it == null) {
                store.set(key.value, increment.value.toValue())
            }
            else {
                when (it) {
                    is IntValue ->
                        store.set(key.value, (it.value.toFloat() + increment.value).toValue())
                    is FloatValue ->
                        store.set(key.value, (it.value + increment.value).toValue())
                }
            }
            store.get(key.value)
        }
    }
}

@WithCommand("SET")
class SetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun execute(store: Store): StoreValue? {
        store.set(key.value, value.toValue())
        return StringValue("OK")
    }
}

@WithCommand("STRLEN")
class StrlenCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue? {
        return withType<StringValue>(store, key.value) {
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

private inline fun <reified T> withType(store: Store, key: String, body: (T?) -> StoreValue?): StoreValue? {
    val value = store.get(key)
    if (value != null) {
        if (value is T) {
            return body(value)
        }
        else {
            throw Command.WrongTypeException(key, value.javaClass.name)
        }
    }
    else {
        return body(value)
    }
}
