package org.botellier.value

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

fun parse(string: String, init: Parser.() -> Unit): Parser = parse(Lexer(string).lex().toList(), init)

/**
 * Parser.
 */

@DslMarker
annotation class ParserMarker

/**
 * Parser class for parsing a list of Lexer tokens. You tell the parser what
 * you expect, and it throws an exception if it finds something else.
 */
@ParserMarker
open class Parser(val tokens: List<Lexer.Token>) {
    var index: Int = 0
        private set
        public get

    /**
     * Function used for parsing the token list from the beginning.
     * The given init lambda has access to all the public parsing
     * functions.
     * @param init the lambda function for parsing.
     */
    fun parse(init: Parser.() -> Unit) {
        index = 0
        this.init()
        if (index < tokens.size) {
            throw ParserException.ParsingException("Not all input was consumed.")
        }
    }

    /**
     * If the next token in the list is an IntToken, return its value. Throws
     * an exception otherwise.
     * @returns the value of the IntToken.
     * @throws ParserException.ParsingException if the value is not the correct token.
     */
    fun int(): Int {
        val token = nextToken()
        if (token is Lexer.IntToken) {
            return token.value
        }
        else {
            throw ParserException.ParsingException("Expected IntToken, found $token")
        }
    }

    /**
     * If the next token in the list is a FloatTOken, return its value. Throws
     * an exception otherwise.
     * @returns the value of the FloatToken.
     * @throws ParserException.ParsingException if the value is not the correct token.
     */
    fun float(): Double {
        val token = nextToken()
        if (token is Lexer.FloatToken) {
            return token.value
        }
        else {
            throw ParserException.ParsingException("Expected FloatToken, found $token")
        }
    }

    /**
     * If the next token in the list is a IntToken or FloatTOken, return its value as
     * a floating point value. Throws an exception otherwise.
     * @returns the numeric value as a floating point value.
     * @throws ParserException.ParsingException if the value is not the correct token.
     */
    fun number(): Double {
        val token = nextToken()
        if (token is Lexer.IntToken) {
            return token.value.toDouble()
        }
        else if (token is Lexer.FloatToken) {
            return token.value
        }
        else {
            throw ParserException.ParsingException("Expected number token, found $token")
        }
    }

    /**
     * If the next token in the list is a StringToken, return its value. Throws
     * an exception otherwise.
     * @param expected lets the function also check for equality to the given value.
     * @returns the value of the StringToken.
     * @throws ParserException.ParsingException if the value is not the correct token.
     */
    fun string(expected: String? = null): String {
        val token = nextToken()
        if (token is Lexer.StringToken) {
            if (expected == null) {
                return token.value
            }
            else if (expected == token.value) {
                return token.value
            }
            else {
                throw ParserException.ParsingException("Expected StringToken \"$expected\", found \"${token.value}\"")
            }
        }
        else {
            throw ParserException.ParsingException("Expected StringToken, found $token")
        }
    }

    /**
     * Accepts any token.
     * @returns the value of the token as 'Any'.
     * @throws ParserException.ParsingException if the token is invalid.
     */
    fun any(): Any {
        val token = nextToken()
        return when (token) {
            is Lexer.IntToken -> token.value
            is Lexer.FloatToken -> token.value
            is Lexer.StringToken -> token.value
            else -> throw ParserException.ParsingException("Invalid token in any().")
        }
    }

    /**
     * Skips the given amount of tokens.
     * @param n the amount of tokens to skip.
     */
    fun skip(n: Int = 0) {
        if (n <= 0) index = tokens.size else index += n
    }

    /**
     * Returns the next token in the list without any
     * type checks.
     * @returns the next token in the list.
     * @throws ParserException.EOFException
     */
    private fun nextToken(): Lexer.Token {
        if (index < tokens.size) {
            return tokens.get(index++)
        }
        else {
            throw ParserException.EOFException()
        }
    }

    // Special combinators.

    inline fun <reified T> many(which: () -> T): List<T> {
        val array = arrayListOf<T>()
        var prev: Int
        while (true) {
            try {
                prev = index
                array.add(which())
            }
            catch (e: ParserException.EOFException) {
                break
            }
            if (prev == index) {
                throw ParserException.ParsingException("Combinator function must consume input.")
            }
        }
        return array.toList()
    }

    inline fun <reified T> many1(which: () -> T): List<T> {
        val array = many(which)
        if (array.isEmpty()) {
            throw ParserException.EOFException()
        }
        return array
    }
}

sealed class ParserException(msg: String) : Throwable(msg) {
    class ParsingException(msg: String) : ParserException(msg)
    class EOFException : ParserException("Unexpected end of input.")
}
