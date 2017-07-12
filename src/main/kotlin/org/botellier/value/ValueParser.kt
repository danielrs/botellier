package org.botellier.value

import java.io.BufferedInputStream

fun parseValue(input: BufferedInputStream): StoreValue {
    return ValueParser(input).parse()
}

fun parseValue(input: ByteArray): StoreValue {
    return ValueParser(input.inputStream().buffered()).parse()
}

class ValueParser(val input: BufferedInputStream) {
    fun parse(): StoreValue {
        val token = Lexer(input).lex()
        return parseToken(token)
    }

    private fun parseToken(token: Lexer.Token): StoreValue {
        return when (token) {
            is Lexer.IntToken -> IntValue(token.value)
            is Lexer.FloatToken -> FloatValue(token.value)
            is Lexer.StringToken -> StringValue(token.value)
            is Lexer.ListToken -> parseListToken(token)
            else -> NilValue()
        }
    }

    private fun parseListToken(token: Lexer.ListToken): ListValue {
        return token.value.map { parseToken(it) as StorePrimitive }.toValue()
    }
}
