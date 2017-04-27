package org.botellier.protocol.command

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
        val result: MutableList<Token> = mutableListOf()

        index = 0
        while (index < string.length) {
            result.add(token())
        }

        return result
    }

    // Lexing functions.
    private fun token(): Token {
        val length = length()
        val tokenString = bulkString(length)
        return castToken(tokenString)
    }

    private fun length(): Int {
        if (string[index] == '$') {
            index++
            try {
                return Integer.parseInt(string())
            }
            catch(e: NumberFormatException) {
                throw LexerException(index, "Invalid length.")
            }
        }
        else {
            throw LexerException(index, "Expected '$'.")
        }
    }

    private fun bulkString(length: Int): String {
        val endIndex = index + length - 1
        val carriageReturn = string.getOrNull(endIndex + 1)
        val lineFeed = string.getOrNull(endIndex + 2)

        if (carriageReturn != '\r' || lineFeed != '\n') {
            throw LexerException(index, "Bulk string of length $length must end with '\\r\\n'.")
        }

        if (index > endIndex) {
            throw LexerException(index, "Bulk strings must have positive length.")
        }

        val substring = string.substring(index..endIndex)
        index = endIndex + 3
        return substring
    }

    private fun string(): String {
        var endIndex = index
        while (string[endIndex] != '\r' && endIndex < string.length) {
            endIndex++
        }
        return bulkString(endIndex - index)
    }

    // Utilities
    private fun castToken(string: String): Token {
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
    enum class TokenType {
        INT,
        FLOAT,
        STRING,
    }

    open abstract class Token(val type: TokenType)
    data class IntToken(val value: Int) : Token(TokenType.INT)
    data class FloatToken(val value: Double) : Token(TokenType.FLOAT)
    data class StringToken(val value: String) : Token(TokenType.STRING)

    class LexerException(index: Int, message: String?) : Throwable("(At [$index]) $message")
}
