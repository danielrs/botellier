package org.botellier.command

// check: http://stackoverflow.com/questions/17848207/making-a-lexical-analyzer
// check: http://llvm.org/docs/tutorial/index.html
class Lexer(val string: String) {
    private var index: Int = 0

    /**
     * Returns a list of tokens from the given string.
     * @return a list of tokens.
     * @throws LexerException if the string format is invalid.
     */
    fun lex(): List<Token> {
        val result = mutableListOf<Token>()

        // Lexing begins here.
        index = 0
        while (index < string.length) {
            when (string[index]) {
                '*' -> result.add(tokenList())
                else -> result.add(token())
            }
        }

        if (result.size == 1 && result.first() is ListToken) {
            return (result.first() as ListToken).value
        }
        else {
            return result
        }
    }

    // Lexing functions.
    private fun token(): PrimitiveToken {
        val length = length('$')
        val tokenString = bulkString(length)
        return castToken(tokenString)
    }

    private fun tokenList(): ListToken {
        val length = length('*')
        val array = ArrayList<PrimitiveToken>(length)
        for (i in 0..length - 1) {
            array.add(token())
        }
        return ListToken(array.toList())
    }

    private fun length(prefix: Char): Int {
        if (string[index] == prefix) {
            index++
            try {
                return Integer.parseInt(string())
            }
            catch(e: NumberFormatException) {
                throw LexerException(index, "Invalid length.")
            }
        }
        else {
            throw LexerException(index, "Expected '$prefix'")
        }
    }

    private fun bulkString(length: Int): String {
        val endIndex = index + length - 1
        val carriageReturn = string.getOrNull(endIndex + 1)
        val lineFeed = string.getOrNull(endIndex + 2)

        if (index > endIndex) {
            throw LexerException(index, "Bulk strings must have positive length.")
        }

        if (endIndex > string.length - 1) {
            throw LexerException(index, "Specified length $length overflows string.")
        }

        if (carriageReturn != '\r' || lineFeed != '\n') {
            throw LexerException(index, "Bulk string of length $length must end with '\\r\\n'.")
        }

        val substring = string.substring(index..endIndex)
        index = endIndex + 3
        return substring
    }

    private fun string(): String {
        var endIndex = index
        while (endIndex < string.length && string[endIndex] != '\r') {
            endIndex++
        }
        return bulkString(endIndex - index)
    }

    // Utilities
    private fun castToken(string: String): PrimitiveToken {
        try {
            if (string.matches(Regex("^[-+]?[0-9]+$"))) {
                return IntToken(string.toInt())
            } else if (string.matches(Regex("^[-+]?[0-9]*\\.[0-9]+$"))) {
                return FloatToken(string.toDouble())
            } else {
                return StringToken(string)
            }
        }
        catch(e: NumberFormatException) {
            throw LexerException(index, "Unable to cast \"$string\" to a token.")
        }
    }

    // Inner classes.
    interface Token
    interface PrimitiveToken : Token

    data class IntToken(val value: Int) : PrimitiveToken
    data class FloatToken(val value: Double) : PrimitiveToken
    data class StringToken(val value: String) : PrimitiveToken
    data class ListToken(val value: List<PrimitiveToken>) : Token

    class LexerException(index: Int, message: String?) : Throwable("(At [$index]) $message")
}
