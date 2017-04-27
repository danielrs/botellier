package org.botellier.protocol.command

@DslMarker
annotation class ParserMarker

@ParserMarker
class Parser(val tokens: List<Lexer.Token>) {
    private var index: Int = 0

    fun parse(init: Parser.() -> Unit) {
        index = 0
        this.init()
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

     fun string(): String {
        val token = token()
        if (token is Lexer.StringToken) {
            return token.value
        }
        else {
            throw ParserException("Expected StringToken, found $token")
        }
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

fun parse(tokens: List<Lexer.Token>, init: Parser.() -> Unit): Parser {
    val parser = Parser(tokens)
    parser.init()
    return parser
}

