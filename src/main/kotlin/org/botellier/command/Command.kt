package org.botellier.command

import org.botellier.server.Client
import org.botellier.server.Server
import org.botellier.store.Store
import org.botellier.store.StoreValue
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

@Target(AnnotationTarget.CLASS)
annotation class WithCommand(val name: String)

@Target(AnnotationTarget.FIELD)
annotation class Parameter(val order: Int, val optional: Boolean = false)

abstract class Command {
    val name: String by lazy {
        val withCommand = this::class.annotations.find { it is WithCommand } as? WithCommand
        withCommand?.name?.toUpperCase() ?: throw InvalidCommandDeclarationException()
    }

    val parameters by lazy {
        this::class.declaredMemberProperties
                .filter {
                    val field = this::class.java.getDeclaredField(it.name)

                    if (field.javaClass.isInstance(CValue::class)) {
                        throw InvalidPropertyException(this::class.toString(), it.name, "must be a CValue")
                    }
                    if (it !is KMutableProperty<*>) {
                        throw InvalidPropertyException(this::class.toString(), it.name, "must be mutable")
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

    // Declaration exceptions.
    class InvalidCommandDeclarationException
        : Throwable("Command must declare a name using the @WithCommand annotation")

    class InvalidPropertyException(className: String, paramName: String, message: String)
        : Throwable("Property '$paramName' from [$className]: $message")

    // Execution exceptions.
    class CommandException(message: String)
        : Throwable(message)

    class CommandDisabledException(name: String)
        : Throwable("Command '$name' is current disabled.")

    class WrongTypeException(key: String, currentType: String)
        : Throwable("Invalid operation on '$key' of type '$currentType'.")
}

/**
 * Command types.
 */

abstract class ConnCommand : Command() {
    open fun execute(server: Server, client: Client): StoreValue {
        throw CommandDisabledException(name)
    }
}

abstract class StoreCommand : Command() {
    open fun execute(store: Store): StoreValue {
        throw CommandDisabledException(name)
    }
}
