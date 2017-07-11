package org.botellier.command

import org.botellier.server.Client
import org.botellier.server.Server
import org.botellier.store.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

@Target(AnnotationTarget.CLASS)
annotation class WithCommand(val name: String)

@Target(AnnotationTarget.FIELD)
annotation class Parameter(val order: Int, val optional: Boolean = false)

abstract class Command {
    val name: String by lazy {
        val withCommand = this::class.annotations.find { it is WithCommand } as? WithCommand
        withCommand?.name?.toUpperCase() ?: throw CommandException.InvalidCommandDeclarationException()
    }

    val parameters by lazy {
        this::class.declaredMemberProperties
                .filter {
                    val field = this::class.java.getDeclaredField(it.name)

                    if (field.javaClass.isInstance(CValue::class)) {
                        throw CommandException.InvalidPropertyException(this::class.toString(), it.name, "must be a CValue")
                    }
                    if (it !is KMutableProperty<*>) {
                        throw CommandException.InvalidPropertyException(this::class.toString(), it.name, "must be mutable")
                    }

                    field.getAnnotation(Parameter::class.java) != null
                }
                .filterIsInstance<KMutableProperty<CValue>>()
                .sortedBy {
                    val field = this::class.java.getDeclaredField(it.name)
                    val parameter = field.getAnnotation(Parameter::class.java)
                    parameter?.order ?: 0
                }
                .map {
                    val field = this::class.java.getDeclaredField(it.name)
                    val parameter = field.getAnnotation(Parameter::class.java)
                    CParameter(this, it, parameter?.optional ?: false)
                }
    }

    // Values for initializing parameters in child classes.
    protected val intValue = CValue.Primitive.Int(0)
    protected val floatValue = CValue.Primitive.Float(0.0)
    protected val stringValue = CValue.Primitive.String("")
    protected val anyValue: CValue.Primitive = CValue.Primitive.Any()

    protected val intArrayValue = CValue.Array.Int(emptyList())
    protected val floatArrayValue = CValue.Array.Float(emptyList())
    protected val stringArrayValue = CValue.Array.String(emptyList())
    protected val anyArrayValue = CValue.Array.Any(emptyList())
    protected val pairArrayValue = CValue.Array.Pair(emptyList())

    // Other.
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("$name ")
        var it = parameters.iterator()
        while (it.hasNext()) {
            builder.append(it.next().get().toString())
            if (it.hasNext()) {
                builder.append(" ")
            }
        }
        return builder.toString()
    }
}

// ----------------
// Command types.
// ----------------

/**
 * Commands that need access to the server.
 */
abstract class ConnCommand : Command() {
    open fun run(server: Server, client: Client): StoreValue {
        throw CommandException.CommandDisabledException(name)
    }
    fun execute(server: Server, client: Client) = run(server, client)
}

/**
 * Commands that has read-only access to the store.
 */
abstract class ReadStoreCommand : Command() {
    open fun run(store: ReadStore): StoreValue {
        throw CommandException.CommandDisabledException(name)
    }
    fun execute(store: ReadStore) = run(store)
}

// TODO: Add logging to mutator functions.
/**
 * Commands that have full access to the store, therefore,
 * all their actions must be logged. The parameter to run
 * is now a StoreTransaction that the command can use for making
 * changes to the store.
 */
abstract class StoreCommand : Command() {
    var transaction : StoreTransaction? = null

    open fun run(transaction: StoreTransaction): StoreValue {
        throw CommandException.CommandDisabledException(name)
    }

    fun transaction(block: StoreTransaction.() -> StoreValue): StoreValue {
        return this.transaction!!.block()
    }

    fun execute(store: Store): StoreValue {
        this.transaction = store.transaction()
        val ret = run(this.transaction!!)
        this.transaction!!.commit()

        return ret
    }
}

// ----------------
// Exceptions.
// ----------------

sealed class CommandException(msg: String) : Throwable(msg) {
    // Declaration exceptions.
    class InvalidCommandDeclarationException
        : CommandException("Command must declare a name using @WIthComand annotation.")

    class InvalidPropertyException(className: String, paramName: String, msg: String)
        : CommandException("Property '$paramName` from [$className]: $msg")

    // Execution exceptions.
    class RuntimeException(msg: String)
        : CommandException(msg)

    class CommandDisabledException(name: String)
        : CommandException("Command '$name' is currently disabled.")

    class WrongTypeException(key: String, currentType: String)
        : CommandException("Invalid operation on '$key' of type '$currentType'.")
}
