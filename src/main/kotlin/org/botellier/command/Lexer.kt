package org.botellier.command

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val CR = '\r'.toInt()
private val LF = '\n'.toInt()
private val DOLLAR = '$'.toInt()
private val STAR = '*'.toInt()

// check: http://stackoverflow.com/questions/17848207/making-a-lexical-analyzer
// check: http://llvm.org/docs/tutorial/index.html
/**
 * Lexer class takes an input stream and lexes byte by byte. Useful for
 * lexing information that is being received through a socket.
 */
class Lexer(bufferedInputStream: BufferedInputStream) {
    private var index = 0
    private val reader: BufferedInputStream = bufferedInputStream

    constructor(inputStream: InputStream) : this(inputStream.buffered())
    constructor(string: String) : this(string.byteInputStream().buffered())

    /**
     * Returns a list of tokens from the given string.
     * @return a list of tokens.
     * @throws LexerException if the string format is invalid.
     */
    fun lex(): List<Token> {
        val result = mutableListOf<Token>()

        // Lexing begins here.
        index = 0
        val char = reader.peek()
        when (char) {
            DOLLAR -> result.add(token())
            STAR -> result.add(tokenList())
            else -> throw LexerException(index, "Expected '$' or '*' found '$char'.")
        }

        if (result.size == 1 && result.first() is ListToken) {
            return (result.first() as ListToken).value
        }
        else {
            return result
        }
    }

    // Lexing functions.
    private fun token(): PrimitiveToken {
        reader.expect(DOLLAR)
        val length = number()
        val bulk = bulkString(length)
        return castToken(bulk)
    }

    private fun tokenList(): ListToken {
        reader.expect(STAR)
        val length = number()
        val array = ArrayList<PrimitiveToken>(length)
        for (i in 0..length - 1) {
            array.add(token())
        }
        return ListToken(array.toList())
    }

    private fun number(): Int {
        val number = reader.readWhile { it != CR }
        reader.expect(LF)
        try {
            return Integer.parseInt(String(number))
        }
        catch (e: NumberFormatException) {
            throw LexerException(index, "Invalid number.")
        }
    }

    private fun bulkString(length: Int): ByteArray {
        if (length <= 0) {
            throw LexerException(index, "Bulk strings must have positive length.")
        }

        val string = reader.readBytes(length)
        reader.expect(CR)
        reader.expect(LF)

        return string
    }

    // Utilities
    private fun castToken(bytes: ByteArray): PrimitiveToken {
        val string = String(bytes)
        try {
            if (string.matches(Regex("^[-+]?[0-9]+$"))) {
                return IntToken(string.toInt())
            } else if (string.matches(Regex("^[-+]?[0-9]*\\.[0-9]+$"))) {
                return FloatToken(string.toDouble())
            } else {
                return StringToken(string)
            }
        }
        catch(e: NumberFormatException) {
            throw LexerException(index, "Unable to cast \"$string\" to a token.")
        }
    }

    // Extension functions.
    private fun BufferedInputStream.readByte(): Int {
        val byte = this.read()
        if (byte != -1) {
            index++
            return byte
        }
        else {
            throw LexerException(index, "Unexpected end of input.")
        }
    }

    private fun BufferedInputStream.peek(): Int {
        this.mark(1)
        val byte = this.read()
        this.reset()
        return byte
    }

    private fun BufferedInputStream.readBytes(length: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        var length = length
        while (length > 0) {
            stream.write(this.readByte())
            length--
        }
        return stream.toByteArray()
    }

    private fun BufferedInputStream.readWhile(pred: (Int) -> Boolean): ByteArray {
        val stream = ByteArrayOutputStream()

        var byte = this.readByte()
        while (pred(byte)) {
            stream.write(byte)
            byte = this.readByte()
        }

        return stream.toByteArray()
    }

    private fun BufferedInputStream.expect(expected: Int): Int {
        val actual = this.readByte()
        if (actual == expected) {
            return actual
        }
        else {
            throw LexerException(index, "Expected '$expected'; found '$actual'.")
        }
    }

    // Inner classes.
    interface Token
    interface PrimitiveToken : Token

    data class IntToken(val value: Int) : PrimitiveToken
    data class FloatToken(val value: Double) : PrimitiveToken
    data class StringToken(val value: String) : PrimitiveToken
    data class ListToken(val value: List<PrimitiveToken>) : Token

    class LexerException(index: Int, message: String?) : Throwable("(At [$index]) $message")
}
