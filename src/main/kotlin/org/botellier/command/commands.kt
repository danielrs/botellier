package org.botellier.command

import org.botellier.server.Client
import org.botellier.server.Server
import org.botellier.store.*
import kotlin.reflect.full.createInstance

// from: https://redis.io/commands

val COMMANDS = arrayOf(
        // Connection.
        AuthCommand::class,
        EchoCommand::class,
        PingCommand::class,
        QuitCommand::class,
        SelectCommand::class,
        // Keys.
        DelCommand::class,
        ExistsCommand::class,
        KeysCommand::class,
        RenameCommand::class,
        TypeCommand::class,
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
        RPopCommand::class,
        RPushCommand::class,
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

private val OK = StringValue("OK")

/**
 * Connection.
 */

@WithCommand("AUTH")
class AuthCommand : ConnCommand() {
    @field:Parameter(0)
    var password = stringValue

    override fun execute(server: Server, client: Client): StoreValue {
        if (server.password == null || password.value == server.password) {
            client.isAuthenticated = true
            return OK
        }
        else {
            throw CommandException("Invalid password")
        }
    }
}

@WithCommand("ECHO")
class EchoCommand : ConnCommand() {
    @field:Parameter(0)
    var message = stringValue

    override fun execute(server: Server, client: Client): StoreValue {
        return StringValue(message.value)
    }
}

@WithCommand("PING")
class PingCommand : ConnCommand() {
    override fun execute(server: Server, client: Client): StoreValue {
        return StringValue("PONG")
    }
}

@WithCommand("QUIT")
class QuitCommand : ConnCommand() {
    override fun execute(server: Server, client: Client): StoreValue {
        return OK
    }
}

@WithCommand("SELECT")
class SelectCommand : ConnCommand() {
    @field:Parameter(0)
    var index = intValue

    override fun execute(server: Server, client: Client): StoreValue {
        if (index.value >= 0 && index.value < server.dbs.size) {
            client.dbIndex = index.value
            return OK
        }
        else {
            throw CommandException("Invalid database index.")
        }
    }
}

/**
 * Keys.
 */

@WithCommand("DEL")
class DelCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun execute(store: Store): StoreValue {
        store.remove(key.value)
        rest.value.map { store.remove(it.value) }
        return OK
    }
}

@WithCommand("EXISTS")
class ExistsCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun execute(store: Store): StoreValue {
        var total = 0
        if (store.get(key.value) !is NilValue) {
            total++
        }
        rest.value.map {
            if (store.get(it.value) !is NilValue) {
                total++
            }
        }
        return IntValue(total)
    }
}

@WithCommand("KEYS")
class KeysCommand : StoreCommand() {
    @field:Parameter(0)
    var pattern = stringValue

    override fun execute(store: Store): StoreValue {
        val regex = Regex(pattern.value)
        val matching = mutableListOf<String>()
        store.keys.map { regex.matches(it) && matching.add(it) }
        return ListValue(matching.map { it.toValue() })
    }
}

@WithCommand("RENAME")
class RenameCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var newkey = stringValue

    override fun execute(store: Store): StoreValue {
        return requireValue<StoreValue>(store, key.value) {
            if (store.get(newkey.value) !is NilValue) {
                val del = DelCommand()
                del.key = newkey
                del.execute(store)
            }
            store.remove(key.value)
            store.set(newkey.value, it)
            OK
        }
    }
}

@WithCommand("TYPE")
class TypeCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        return StringValue(store.get(key.value)::class.simpleName ?: "Unknown")
    }
}

/**
 * Lists.
 */

@WithCommand("LINDEX")
class LIndexCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var index = intValue

    override fun execute(store: Store): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            if (index.value >= 0 && index.value <= it.size - 1) {
                it.get(index.value)
            }
            else {
                NilValue()
            }
        }
    }
}

@WithCommand("LINSERT")
class LInsertCommand : StoreCommand() {
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
class LLenCommand : StoreCommand() {
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
class LPopCommand : StoreCommand() {
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
class LPushCommand : StoreCommand() {
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
class LRangeCommand : StoreCommand() {
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
class LRemCommand : StoreCommand() {
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
                    for (i in it.size-1 downTo 0) {
                        if (count == 0) break
                        if (value == it.get(i)) {
                            indicesToRemove.add(i)
                            count++
                        }
                    }
                }
                count > 0 -> {
                    for (i in 0..it.size-1) {
                        if (count == 0) break
                        if (value == it.get(i)) {
                            indicesToRemove.add(i)
                            count--
                        }
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
class LSetCommand : StoreCommand() {
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
            OK
        }
    }
}

@WithCommand("LTRIM")
class LTrimCommand : StoreCommand() {
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
            OK
        }
    }
}

@WithCommand("RPOP")
class RPopCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun execute(store: Store): StoreValue {
        return withValue<ListValue>(store, key.value) {
            if (it != null && it.size > 0) {
                it.rpop()
            }
            else {
                NilValue()
            }
        }
    }
}

@WithCommand("RPUSH")
class RPushCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    @field:Parameter(2)
    var rest = anyArrayValue

    override fun execute(store: Store): StoreValue {
        return withValue<ListValue>(store, key.value) {
            val list = it ?: ListValue()
            list.rpush(value.toValue())
            rest.value.map { list.rpush(it.toValue()) }
            store.set(key.value, list)
            IntValue(list.size)
        }
    }
}

/**
 * Strings.
 */

@WithCommand("APPEND")
class AppendCommand : StoreCommand() {
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
class DecrCommand : StoreCommand() {
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
class DecrbyCommand : StoreCommand() {
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
class GetCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue
    override fun execute(store: Store): StoreValue {
        return withPrimitive<StorePrimitive>(store, key.value) {
            it ?: NilValue()
        }
    }
}

@WithCommand("INCR")
class IncrCommand : StoreCommand() {
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
class IncrbyCommand : StoreCommand() {
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
class IncrbyfloatCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = floatValue

    override fun execute(store: Store): StoreValue {
        return withValue<StoreNumber>(store, key.value) {
            val value = when(it) {
                is IntValue -> (it.value.toFloat() + increment.value).toValue()
                is FloatValue -> (it.value + increment.value).toValue()
                else -> increment.value.toValue()
            }
            store.set(key.value, value)
            value
        }
    }
}

@WithCommand("MGET")
class MGetCommand : StoreCommand() {
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
class MSetCommand : StoreCommand() {
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
        return OK
    }
}

@WithCommand("SET")
class SetCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun execute(store: Store): StoreValue {
        store.set(key.value, value.toValue())
        return OK
    }
}

@WithCommand("STRLEN")
class StrlenCommand : StoreCommand() {
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
