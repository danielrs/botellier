package org.botellier.protocol.command.parser

import org.botellier.protocol.command.Lexer

// General parsing functions.
fun <TInput, TOutput> succeed(value: TOutput): Parser<TInput, TOutput> {
    return Parser({ input -> Result(value, input)})
}

fun <TInput, TOutput> repeat(parser: Parser<TInput, TOutput>): Parser<TInput, List<TOutput>> {
    return repeat1(parser) or succeed<TInput, List<TOutput>>(emptyList())
}

fun <TInput, TOutput> repeat1(parser: Parser<TInput, TOutput>): Parser<TInput, List<TOutput>> {
    return parser.flatMap(
            { _ -> repeat(parser) },

            { value: TOutput, list: List<TOutput> ->
                mutableListOf(value) + list
            }
    )
}

// Token parsing functions.
val anyToken: Parser<List<Lexer.Token>, Lexer.Token> = Parser({
    if (it.isNotEmpty()) {
        Result(it.first(), it.drop(1))
    }
    else {
        null
    }
})

val anyIntToken: Parser<List<Lexer.Token>, Lexer.Token> = Parser({
    if (it.isNotEmpty() && it[0] is Lexer.IntToken) {
        Result(it.first(), it.drop(1))
    }
    else {
        null
    }
})

val anyFloatToken: Parser<List<Lexer.Token>, Lexer.Token> = Parser({
    if (it.isNotEmpty() && it[0] is Lexer.FloatToken) {
        Result(it.first(), it.drop(1))
    }
    else {
        null
    }
})

val anyStringToken: Parser<List<Lexer.Token>, Lexer.Token> = Parser({
    if (it.isNotEmpty() && it[0] is Lexer.StringToken) {
        Result(it.first(), it.drop(1))
    }
    else {
        null
    }
})

fun stringToken(string: String): Parser<List<Lexer.Token>, Lexer.Token> {
    return anyStringToken.filter {
        token -> token is Lexer.StringToken && token.value == string
    }
}
