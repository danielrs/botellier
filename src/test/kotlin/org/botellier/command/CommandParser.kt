package org.botellier.command

import org.botellier.serializer.toByteArray
import org.botellier.store.toValue

import org.junit.Assert
import org.junit.Test

class CommandParserTest {

    /**
     * Success cases
     */

    @Test
    fun appendCommand() {
        val tokens = toTokens("APPEND", "key", "10")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is AppendCommand)
    }

    @Test
    fun decrCommand() {
        val tokens = toTokens("DECR", "key")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is DecrCommand)
    }

    @Test
    fun decrbyCommand() {
        val tokens = toTokens("DECRBY", "key", "10")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is DecrbyCommand)
    }

    @Test
    fun getCommand() {
        val tokens = toTokens("GET", "key")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is GetCommand)
    }

    @Test
    fun incrCommand() {
        val tokens = toTokens("INCR", "key")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is IncrCommand)
    }

    @Test
    fun incrbyCommand() {
        val tokens = toTokens("INCRBY", "key", "10")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is IncrbyCommand)
    }

    @Test
    fun incrbyfloatCommand() {
        val tokens = toTokens("INCRBYFLOAT", "key", "10.0")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is IncrbyfloatCommand)
    }

    @Test
    fun setCommand() {
        val tokens = toTokens("SET", "key", "10")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is SetCommand)
    }

    @Test
    fun strlenCommand() {
        val tokens = toTokens("STRLEN", "key")
        val command = CommandParser.parse(tokens)
        Assert.assertTrue(command is StrlenCommand)
    }

    /**
     * Fail cases.
     */

    @Test
    fun appendCommandFail() {
        commandShouldFail("APPEND", "KEY")
    }

    @Test
    fun decrCommandFail() {
        commandShouldFail("DECR")
    }

    @Test
    fun decrbyCommandFail() {
        commandShouldFail("DECRBY", "key")
    }

    @Test
    fun getCommandFail() {
        commandShouldFail("GET")
    }

    @Test
    fun getCommandKeyFail() {
        commandShouldFail("GET", "1")
    }

    @Test
    fun incrCommandFail() {
        commandShouldFail("INCR")
    }

    @Test
    fun incrbyCommandFail() {
        commandShouldFail("INCRBY", "key")
    }

    @Test
    fun incrbyfloatCommandFail() {
        commandShouldFail("INCRBYFLOAT", "key")
    }

    @Test
    fun incrbyfloatCommandIntFail() {
        commandShouldFail("INCRBYFLOAT", "key", "10")
    }

    @Test
    fun setCommandFail() {
        commandShouldFail("SET")
    }

    @Test
    fun setCommandIntFail() {
        commandShouldFail("SET", "1")
    }

    @Test
    fun strlenCommandFail() {
        commandShouldFail("STRLEN")
    }

    /**
     * Utility functions.
     */

    private fun toTokens(items: List<String>): List<Lexer.Token> {
        val listValue = items.map { it.toValue() }.toValue()
        return Lexer(String(listValue.toByteArray())).lex()
    }

    private fun toTokens(vararg items: String): List<Lexer.Token> = toTokens(items.toList())

    private fun commandShouldFail(vararg items: String) {
        val tokens = toTokens(items.toList())
        try {
            CommandParser.parse(tokens)
            Assert.fail("Invalid command $items parsed.")
        }
        catch (e: Throwable) {}
    }
}
