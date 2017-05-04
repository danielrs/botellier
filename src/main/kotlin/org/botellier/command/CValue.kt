package org.botellier.command

// Types allowed in command parameters.
sealed class CValue {
    companion object {
        fun primitive(value: Any): CValue {
            return when(value) {
                is Int -> CValue.Primitive.Int(value)
                is Float -> CValue.Primitive.Float(value.toDouble())
                is Double -> CValue.Primitive.Float(value)
                is String -> CValue.Primitive.String(value)
                else -> throw InvalidPrimitiveException(value)
            }
        }
    }

    sealed class Primitive : CValue() {
        data class Int(val value: kotlin.Int) : Primitive()
        data class Float(val value: kotlin.Double) : Primitive()
        data class String(val value: kotlin.String) : Primitive()
        class Any : Primitive()
    }

    sealed class Array : CValue() {
        data class Int(val value: kotlin.Array<Primitive.Int>) : Array()
        data class Float(val value: kotlin.Array<Primitive.Float>) : Array()
        data class String(val value: kotlin.Array<Primitive.String>) : Array()
        data class Any(val value: kotlin.Array<Primitive>) : Array()
    }

    data class Pair(val first: Primitive, val second: Primitive) : CValue()

    override final fun toString(): String {
        return when(this) {
            is Primitive.Int -> "${this.value}:Int"
            is Primitive.Float -> "${this.value}:Float"
            is Primitive.String -> "\"${this.value}\":String"
            is Primitive.Any -> "Any:Any"
            is Array.Int -> "${this.value}:IntArray"
            is Array.Float -> "${this.value}:FloatArray"
            is Array.String -> "${this.value}:StringArray"
            is Array.Any -> "${this.value}:AnyArray"
            is Pair -> "(${this.first}, ${this.second}):Pair"
        }
    }

    // Exceptions.
    class InvalidPrimitiveException(value: Any)
        : Throwable("Cannot construct a command primitive using $value.")
}
