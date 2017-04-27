package org.botellier.protocol.command

import org.junit.Assert
import org.junit.Test

class ParserTest {

    @Test
    fun parseInt() {
        val tokens = Lexer("$3\r\n100\r\n").lex()
        var value = 0

        parse(tokens) {
            value = int()
        }

        Assert.assertEquals(value, 100)
    }

    @Test
    fun parseFloat() {
        val tokens = Lexer("$4\r\n10.1\r\n").lex()
        var value = 0.0

        parse(tokens) {
            value = float()
        }

        Assert.assertEquals(value, 10.1, 0.001)
    }

    @Test
    fun parseString() {
        val tokens = Lexer("$3\r\none\r\n").lex()
        var value = ""

        parse(tokens) {
            value = string()
        }

        Assert.assertEquals(value, "one")
    }

    @Test
    fun parseList() {
        val tokens = Lexer("$3\r\none\r\n$3\r\ntwo\r\n$5\r\nthree\r\n").lex()
        val values = mutableListOf<String>()

        parse(tokens) {
            while (true) {
                try { values.add(string()) } catch(e: Throwable) { break }
            }
        }

        Assert.assertEquals(values, listOf("one", "two", "three"))
    }
}
