package org.botellier.value

import org.junit.Assert
import org.junit.Test

class LexerTest {
    @Test
    fun singleString() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { intToken(it, 1) }
        )
        val result = Lexer("*1\r\n$1\r\n1\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun specialString() {
        val string = "特è"
        val byteCount = string.toByteArray().size
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, string) }
        )
        val result = Lexer("*1\r\n$$byteCount\r\n$string\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun multipleStrings() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, "SET") },
                { stringToken(it, "counter") },
                { stringToken(it, "one") }
        )
        val result = Lexer("*3\r\n$3\r\nSET\r\n$7\r\ncounter\r\n$3\r\none\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun integer() {
         val expected = listOf<(Lexer.Token) -> Boolean>(
                 { stringToken(it, "LPUSH") },
                 { stringToken(it, "list") },
                 { intToken(it, 1000) }
        )
        val result = Lexer("*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$4\r\n1000\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun float() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, "LPUSH") },
                { stringToken(it, "list") },
                { floatToken(it, 104.1) }
        )
        val result = Lexer("*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$5\r\n104.1\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun pointFloat() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, "LPUSH") },
                { stringToken(it, "list") },
                { floatToken(it, 0.1) }
        )
        val result = Lexer("*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$2\r\n.1\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun integerAndFloat() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, "HSET") },
                { stringToken(it, "a") },
                { intToken(it, 10) },
                { stringToken(it, "b") },
                { floatToken(it, 10.1) }
        )
        val result = Lexer("*5\r\n$4\r\nHSET\r\n$1\r\na\r\n$2\r\n10\r\n$1\r\nb\r\n$4\r\n10.1\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun floatAndInteger() {
        val expected = listOf<(Lexer.Token) -> Boolean>(
                { stringToken(it, "HSET") },
                { stringToken(it, "a") },
                { floatToken(it, 10.1) },
                { stringToken(it, "b") },
                { intToken(it, 10) }
        )
        val result = Lexer("*5\r\n$4\r\nHSET\r\n$1\r\na\r\n$4\r\n10.1\r\n$1\r\nb\r\n$2\r\n10\r\n").lex().toList()
        assertTokens(result, expected)
    }

    @Test
    fun emptyString() {
        try {
            Lexer("").lex()
            Assert.fail("Passed empty string.")
        }
        catch (_: Throwable) {}
    }

    @Test
    fun incompleteLength() {
        try {
            val tokens = Lexer("$\r\n").lex()
            Assert.fail("Invalid length passed")
        }
        catch (_: Throwable) {}
    }

    /**
     * Utilities.
     */

    private fun intToken(token: Lexer.Token, value: Int): Boolean {
        return token is Lexer.IntToken && token.value == value
    }

    private fun floatToken(token: Lexer.Token, value: Double): Boolean {
        return token is Lexer.FloatToken && token.value == value
    }

    private fun stringToken(token: Lexer.Token, value: String): Boolean {
        return token is Lexer.StringToken && token.value == value
    }

    private fun assertTokens(tokens: List<Lexer.Token>, expected: List<(Lexer.Token) -> Boolean>) {
        Assert.assertEquals(tokens.size, expected.size)
        for (i in tokens.indices) {
            Assert.assertTrue(expected[i](tokens[i]))
        }
    }
}
