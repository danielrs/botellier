package org.botellier.command

import org.botellier.server.Client
import org.botellier.server.Server
import org.botellier.store.*
import org.botellier.value.*
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

// ----------------
// Connection.
// ----------------

@WithCommand("AUTH")
class AuthCommand : ConnCommand() {
    @field:Parameter(0)
    var password = stringValue

    override fun run(server: Server, client: Client): StoreValue {
        if (server.password == null || password.value == server.password) {
            client.isAuthenticated = true
            return OK
        }
        else {
            throw CommandException.RuntimeException("Invalid password")
        }
    }
}

@WithCommand("ECHO")
class EchoCommand : ConnCommand() {
    @field:Parameter(0)
    var message = stringValue

    override fun run(server: Server, client: Client): StoreValue {
        return StringValue(message.value)
    }
}

@WithCommand("PING")
class PingCommand : ConnCommand() {
    override fun run(server: Server, client: Client): StoreValue {
        return StringValue("PONG")
    }
}

@WithCommand("QUIT")
class QuitCommand : ConnCommand() {
    override fun run(server: Server, client: Client): StoreValue {
        return OK
    }
}

@WithCommand("SELECT")
class SelectCommand : ConnCommand() {
    @field:Parameter(0)
    var index = intValue

    override fun run(server: Server, client: Client): StoreValue {
        if (index.value >= 0 && index.value < server.dbs.size) {
            client.dbIndex = index.value
            return OK
        }
        else {
            throw CommandException.RuntimeException("Invalid database index.")
        }
    }
}

// ----------------
// Keys.
// ----------------

@WithCommand("DEL")
class DelCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transaction {
            delete(key.value)
            rest.value.map { delete(it.value) }
            OK
        }
    }
}

@WithCommand("EXISTS")
class ExistsCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun run(store: ReadStore): StoreValue {
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
class KeysCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var pattern = stringValue

    override fun run(store: ReadStore): StoreValue {
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

    override fun run(transaction: StoreTransaction): StoreValue {
        val oldValue = transaction.get(key.value)

        return transaction {
            delete(key.value)
            set(newkey.value, oldValue)
            OK
        }
    }
}

@WithCommand("TYPE")
class TypeCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(store: ReadStore): StoreValue {
        return StringValue(store.get(key.value)::class.simpleName ?: "Unknown")
    }
}

/**
 * Lists.
 */

@WithCommand("LINDEX")
class LIndexCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var index = intValue

    override fun run(store: ReadStore): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            if (index.value >= 0 && index.value <= it.size - 1) {
                it.unwrap().get(index.value)
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

    override fun run(transaction: StoreTransaction): StoreValue {
        if (position.value !in listOf("BEFORE", "AFTER")) {
            throw CommandException.RuntimeException("LINSERT [BEFORE|AFTER] expected.")
        }

        val updated = transaction.update<ListValue>(key.value) {
            val isBefore = position.value == "BEFORE"
            val value = value.toValue()
            val index = it.indexOf(pivot.toValue())

            when (index) {
                -1 -> return IntValue(-1)
                else -> {
                    it.copy { when {
                        isBefore -> it.add(index, value)
                        index < it.size - 1 -> it.add(index + 1, value)
                        else -> it.rpush(value)
                    }}
                }
            }
        }

        return IntValue((updated as ListValue).size)
    }
}

@WithCommand("LLEN")
class LLenCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(store: ReadStore): StoreValue {
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

    override fun run(transaction: StoreTransaction): StoreValue {
        var ret: StoreValue = NilValue()

        transaction.mupdate<ListValue>(key.value) {
            if (it != null && it.size > 0) {
                it.copy {
                    ret = it.lpop()
                }
            } else {
                NilValue()
            }
        }

        return ret
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

    override fun run(transaction: StoreTransaction): StoreValue {
        val updated = transaction.mupdate<ListValue>(key.value) {
            val current = it ?: ListValue()
            current.copy {  list ->
                list.lpush(value.toValue())
                rest.value.map { list.lpush(it.toValue()) }
            }
        }

        return (updated as ListValue).size.toValue()
    }
}

@WithCommand("LRANGE")
class LRangeCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var start = intValue

    @field:Parameter(2)
    var stop = intValue

    override fun run(store: ReadStore): StoreValue {
        return requireValue<ListValue>(store, key.value) {
            it.unwrap().slice(start.value, stop.value).toValue()
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

    override fun run(transaction: StoreTransaction): StoreValue {
        val updated = transaction.update<ListValue>(key.value) {
            val count = count.value
            val value = value.toValue()

            val indices = it.mapIndexed { i, elem -> Pair(i, elem) }.filter { it.second == value }.map { it.first }
            when {
                count < 0 -> {
                    it.copy { it.remove(indices.takeLast(Math.abs(count))) }
                }
                count > 0 -> {
                    it.copy { it.remove(indices.take(Math.abs(count))) }
                }
                else -> {
                    it.copy { it.remove(indices) }
                }
            }
        }

        return IntValue((updated as ListValue).size)
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

    override fun run(transaction: StoreTransaction): StoreValue {
        transaction.update<ListValue>(key.value) {
            if (index.value < 0 || index.value > it.size - 1) {
                throw CommandException.RuntimeException("LSET index out of bounds")
            }

            it.copy {
                it.set(index.value, value.toValue())
            }
        }

        return OK
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

    override fun run(transaction: StoreTransaction): StoreValue {
        transaction.update<ListValue>(key.value) {
            it.copy { list -> list.trim(start.value, stop.value) }
        }
        return OK
    }
}

@WithCommand("RPOP")
class RPopCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(transaction: StoreTransaction): StoreValue {
        var ret: StoreValue = NilValue()

        transaction.mupdate<ListValue>(key.value) {
            if (it != null && it.size > 0) {
                it.copy {
                    ret = it.rpop()
                }
            } else {
                NilValue()
            }
        }

        return ret
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

    override fun run(transaction: StoreTransaction): StoreValue {
        val updated = transaction.mupdate<ListValue>(key.value) {
            val current = it ?: ListValue()
            current.copy { list ->
                list.rpush(value.toValue())
                rest.value.map { list.rpush(it.toValue()) }
            }
        }

        return IntValue((updated as ListValue).size)
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

    override fun run(transaction: StoreTransaction): StoreValue {
        val updated = transaction.mupdate<StringValue>(key.value) {
            val builder = StringBuilder()
            if (it != null) {
                builder.append(it.unwrap())
            }
            builder.append(value.toString())
            builder.toString().toValue()
        }

        return (updated as StringValue).unwrap().length.toValue()
    }
}

@WithCommand("DECR")
class DecrCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transactionIncr(transaction, key.value, -1)
    }
}

@WithCommand("DECRBY")
class DecrbyCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var decrement = intValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transactionIncr(transaction, key.value, -decrement.value)
    }
}

@WithCommand("GET")
class GetCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue
    override fun run(store: ReadStore): StoreValue {
        return withPrimitive<StorePrimitive>(store, key.value) {
            it ?: NilValue()
        }
    }
}

@WithCommand("INCR")
class IncrCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transactionIncr(transaction, key.value, 1)
    }
}

@WithCommand("INCRBY")
class IncrbyCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = intValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transactionIncr(transaction, key.value, increment.value)
    }
}

@WithCommand("INCRBYFLOAT")
class IncrbyfloatCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = floatValue

    override fun run(transaction: StoreTransaction): StoreValue {
        return transactionIncrFloat(transaction, key.value, increment.value)
    }
}

@WithCommand("MGET")
class MGetCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var rest = stringArrayValue

    override fun run(store: ReadStore): StoreValue {
        val list = mutableListOf<StoreValue>()

        list.rpush(withPrimitive<StorePrimitive>(store, key.value) { it ?: NilValue() })
        rest.value.map {
            list.rpush(withPrimitive<StorePrimitive>(store, it.value) { it ?: NilValue() })
        }

        return list.map { it as StorePrimitive }.toValue()
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

    override fun run(transaction: StoreTransaction): StoreValue {
        return transaction {
            set(key.value, value.toValue())
            for ((key, value) in rest.value) {
                set(key, value.toValue())
            }
            OK
        }
    }
}

@WithCommand("SET")
class SetCommand : StoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue

    override fun run(transaction: StoreTransaction): StoreValue {
        transaction.set(key.value, value.toValue())
        return OK
    }
}

@WithCommand("STRLEN")
class StrlenCommand : ReadStoreCommand() {
    @field:Parameter(0)
    var key = stringValue

    override fun run(store: ReadStore): StoreValue {
        return withValue<StringValue>(store, key.value) {
            if (it != null) {
                IntValue(it.unwrap().length)
            }
            else {
                IntValue(0)
            }
        }
    }
}

// ----------------
// Utility functions.
// ----------------

private fun transactionIncr(transaction: StoreTransaction, key: String, incr: Int): StoreValue {
    return transaction.mupdate<StoreNumber>(key) {
        when (it) {
            is IntValue -> IntValue(it.unwrap() + incr)
            is FloatValue -> FloatValue(it.unwrap() + incr.toFloat())
            else -> IntValue(incr)
        }
    }
}

private fun transactionIncrFloat(transaction: StoreTransaction, key: String, incr: Double): StoreValue {
    return transaction.mupdate<StoreNumber>(key) {
        when (it) {
            is IntValue -> FloatValue(it.unwrap().toFloat() + incr)
            is FloatValue -> FloatValue(it.unwrap() + incr)
            else -> FloatValue(incr)
        }
    }
}

/**
 * Utility functions.
 */

/**
 * Gets 'key' from store and throws exception if 'key' doesn't exists.
 */
private inline fun <reified T, R> requireType(store: ReadStore, key: String, body: (T) -> R): R {
    val value = store.get(key)
    if (value !is NilValue) {
        if (value is T) {
            return body(value)
        }
        else {
            throw CommandException.WrongTypeException(key, value.javaClass.name)
        }
    }
    else {
        throw CommandException.WrongTypeException(key, NilValue::class.qualifiedName ?: "NilValue")
    }
}

/**
 * Just like [requireType], except that body parameter is nullable (doesn't throw exception if 'key' lookup
 * fails).
 */
private inline fun <reified T, R> withType(store: ReadStore, key: String, body: (T?) -> R): R {
    val value = store.get(key)
    if (value !is NilValue) {
        if (value is T) {
            return body(value)
        }
        else {
            throw CommandException.WrongTypeException(key, value.javaClass.name)
        }
    }
    else {
        return body(null)
    }
}

private inline fun <reified T> withValue(store: ReadStore, key: String, body: (T?) -> StoreValue)
        = withType(store, key, body)

private inline fun <reified T> withPrimitive(store: ReadStore, key: String, body: (T?) -> StorePrimitive)
        = withType(store, key, body)

private inline fun <reified T> requireValue(store: ReadStore, key: String, body: (T) -> StoreValue)
        = requireType(store, key, body)
