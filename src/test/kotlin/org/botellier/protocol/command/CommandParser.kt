package org.botellier.protocol.command

import org.junit.Assert
import org.junit.Test

class CommandParserTest {
    @Test
    fun appendCommand() {
        Assert.fail()
    }

    @Test
    fun decrCommand() {
        Assert.fail()
    }

    @Test
    fun decrbyCommand() {
        Assert.fail()
    }

    @Test
    fun getCommand() {
        Assert.fail()
    }

    @Test
    fun incrCommand() {
        Assert.fail()
    }

    @Test
    fun incrbyCommand() {
        Assert.fail()
    }

    @Test
    fun incrbyfloatCommand() {
        Assert.fail()
    }

    @Test
    fun setCommand() {
        val tokens = Lexer("$3\r\nSET\r\n$3\r\nkey\r\n$2\r\n10\r\n").lex()
        val command = CommandParser.parse(tokens)
        println(command)
        Assert.assertTrue(command is SetCommand)
    }

    @Test
    fun strlenCommand() {
        Assert.fail()
    }
}
