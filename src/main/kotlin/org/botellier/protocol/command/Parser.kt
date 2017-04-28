package org.botellier.protocol.command

import kotlin.reflect.full.createInstance

/**
 * Parsing functions for convenience.
 */

fun parseCommand(tokens: List<Lexer.Token>): Command {

    val firstToken = tokens.first()

    if (firstToken is Lexer.StringToken) {
        val commandClass = COMMANDS[firstToken.value.toUpperCase()]
        if (commandClass != null) {

            val command = commandClass.createInstance()
            parseTokens(tokens.drop(1)) {
                parameters@ for (p in command.parameters) {
                    when {
                        p.isInt -> p.set(CValue.primitive(int()))
                        p.isFloat -> p.set(CValue.primitive(float()))
                        p.isString -> p.set(CValue.primitive(string()))
                        p.isAny -> p.set(CValue.primitive(any()))
                    }
                }
            }
            return command

        }
    }

    throw Parser.ParserException("Unknown command: $tokens")
}

fun parse(string: String, init: Parser.() -> Unit): Parser = parseTokens(Lexer(string).lex(), init)

fun parseTokens(tokens: List<Lexer.Token>, init: Parser.() -> Unit): Parser {
    val parser = Parser(tokens)
    parser.init()
    return parser
}

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
