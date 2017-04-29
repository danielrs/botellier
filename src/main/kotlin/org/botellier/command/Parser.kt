package org.botellier.command

/**
 * Takes a list of tokens and an extension lambda to parse the given tokens.
 * @param tokens the list of Lexer.Token values.
 * @param init the extension lambda.
 * @return A parser that contains the given tokens.
 */
fun parse(tokens: List<Lexer.Token>, init: Parser.() -> Unit): Parser {
    val parser = Parser(tokens)
    parser.parse(init)
    return parser
}

fun parse(string: String, init: Parser.() -> Unit): Parser = parse(Lexer(string).lex(), init)

/**
 * Parser.
 */

@DslMarker
annotation class ParserMarker

@ParserMarker
open class Parser(val tokens: List<Lexer.Token>) {
    private var index: Int = 0

    fun parse(init: Parser.() -> Unit) {
        index = 0
        this.init()

        if (index < tokens.size) {
            throw ParserException("Not all input was consumed.")
        }
    }

    fun int(): Int {
        val token = token()
        if (token is Lexer.IntToken) {
            return token.value
        }
        else {
            throw ParserException("Expected IntToken, found $token")
        }
    }

    fun float(): Double {
        val token = token()
        if (token is Lexer.FloatToken) {
            return token.value
        }
        else {
            throw ParserException("Expected FloatToken, found $token")
        }
    }

    fun number(): Double {
        val token = token()
        if (token is Lexer.IntToken) {
            return token.value.toDouble()
        }
        else if (token is Lexer.FloatToken) {
            return token.value
        }
        else {
            throw ParserException("Expected number token, found $token")
        }
    }


    fun string(expected: String? = null): String {
        val token = token()
        if (token is Lexer.StringToken) {
            if (expected == null) {
                return token.value
            }
            else if (expected == token.value) {
                return token.value
            }
            else {
                throw ParserException("Expected StringToken \"$expected\", found \"${token.value}\"")
            }
        }
        else {
            throw ParserException("Expected StringToken, found $token")
        }
    }

    fun any(): Any {
        val token = token()
        return when (token) {
            is Lexer.IntToken -> token.value
            is Lexer.FloatToken -> token.value
            is Lexer.StringToken -> token.value
            else -> throw ParserException("Invalid token in any().")
        }
    }

    fun skip(n: Int = 0) {
        if (n <= 0) index = tokens.size else index += n
    }

    private fun token(): Lexer.Token {
        if (index < tokens.size) {
            return tokens.get(index++)
        }
        else {
            throw ParserException("Unexpected end of input.")
        }
    }

    // Exceptions.
    class ParserException(message: String) : Throwable(message)
}
