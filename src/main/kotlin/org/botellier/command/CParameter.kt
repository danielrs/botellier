package org.botellier.command

import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

class CParameter(private val command: Command, private val property: KMutableProperty<CValue>, val isOptional: Boolean) {
    val name: String = property.name

    fun get(): Any {
        return property.getter.call(command)
    }

    fun set(value: CValue) {
        property.setter.call(command, value)
    }

    // Checking functions.
    val type = property.returnType

    val isInt: Boolean = property.returnType.isSubtypeOf(CValue.Primitive.Int::class.createType())
    val isFloat: Boolean = property.returnType.isSubtypeOf(CValue.Primitive.Float::class.createType())
    val isString: Boolean = property.returnType.isSubtypeOf(CValue.Primitive.String::class.createType())
    val isAny: Boolean = property.returnType.isSubtypeOf(CValue.Primitive::class.createType())

    val isIntArray: Boolean = property.returnType.isSubtypeOf(CValue.Array.Int::class.createType())
    val isFloatArray: Boolean = property.returnType.isSubtypeOf(CValue.Array.Float::class.createType())
    val isStringArray: Boolean = property.returnType.isSubtypeOf(CValue.Array.String::class.createType())
    val isAnyArray: Boolean = property.returnType.isSubtypeOf(CValue.Array::class.createType())
}
