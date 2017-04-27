package org.botellier.protocol.command.parser

import org.botellier.protocol.command.Lexer
import org.junit.Assert
import org.junit.Test

class ParserTest {

    val string = "$3\r\nSET\r\n$3\r\nkey\r\n$2\r\n10\r\n"

    @Test
    fun anyToken() {
        val tokens = Lexer(string).lex()

        val (command, rest0) = stringToken("SET")(tokens)!!
        val parser = stringToken("SET") then repeat(anyToken)

        println(parser(tokens))
    }
}
