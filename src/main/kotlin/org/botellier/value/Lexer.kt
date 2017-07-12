package org.botellier.value

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream

private val CR = '\r'.toInt()
private val LF = '\n'.toInt()
private val COLON = ':'.toInt()
private val SEMICOLON = ';'.toInt()
private val DOLLAR = '$'.toInt()
private val STAR = '*'.toInt()

// check: http://stackoverflow.com/questions/17848207/making-a-lexical-analyzer
// check: http://llvm.org/docs/tutorial/index.html
/**
 * Lexer class takes an input stream and lexes byte by byte. Useful for
 * reading information that is being received through a socket.
 * @param bufferedInputStream the buffered input stream (required for peeking values
 * and such).
 */
class Lexer(bufferedInputStream: BufferedInputStream) {
    private var index = 0
    private val reader = bufferedInputStream

    constructor(string: String) : this(string.byteInputStream().buffered())

    /**
     * Returns a token.
     * @return a token.
     * @throws LexerException if the string format is invalid.
     */
    fun lex(): Token {
        index = 0
        return token()
    }

    // Lexing functions.
    private fun token(): Token {
        val next = reader.peek()
        return when(next) {
            COLON -> intToken()
            SEMICOLON -> floatToken()
            DOLLAR -> stringToken()
            STAR -> listToken()
            else -> throw LexerException.LexingException(index, "Expected one of ':', ';', '$', '*'; found '$next'.")
        }
    }

    private fun primitiveToken(): PrimitiveToken {
        val next = reader.peek()
        return when(next) {
            COLON -> intToken()
            SEMICOLON -> floatToken()
            DOLLAR -> stringToken()
            else -> throw LexerException.LexingException(index, "Expected one of ':', ';', '$'; found '$next'.")
        }
    }

    private fun intToken(): PrimitiveToken {
        reader.expect(COLON)
        return IntToken(int())
    }

    private fun floatToken(): PrimitiveToken {
        reader.expect(SEMICOLON)
        return FloatToken(float())
    }

    private fun stringToken(): PrimitiveToken {
        reader.expect(DOLLAR)
        val length = int()
        val bulk = bulkString(length)
        return castToken(bulk)
    }

    private fun listToken(): ListToken {
        reader.expect(STAR)
        val length = int()
        val array = ArrayList<PrimitiveToken>(length)
        for (i in 0..length - 1) {
            array.add(primitiveToken())
        }
        return ListToken(array.toList())
    }

    private fun int(): Int {
        val number = reader.readWhile { it != CR }
        reader.expect(CR)
        reader.expect(LF)
        try {
            return String(number).toInt()
        } catch (e: NumberFormatException) {
            throw LexerException.LexingException(index, "Invalid integer number.")
        }
    }

    private fun float(): Double {
        val number = reader.readWhile { it != CR }
        reader.expect(CR)
        reader.expect(LF)
        try {
            return String(number).toDouble()
        } catch (e: NumberFormatException) {
            throw LexerException.LexingException(index, "Invalid floating point number.")
        }
    }

    private fun bulkString(length: Int): ByteArray {
        if (length <= 0) {
            throw LexerException.LexingException(index, "Bulk strings must have positive length.")
        }

        val string = reader.readBytes(length)
        reader.expect(CR)
        reader.expect(LF)

        return string
    }

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
            throw LexerException.LexingException(index, "Unable to cast \"$string\" to a token.")
        }
    }

    // ----------------
    // Extension functions for BufferedInputStream.
    // ----------------

    private fun BufferedInputStream.readByte(): Int {
        val byte = this.read()
        if (byte != -1) {
            index++
            return byte
        }
        else {
            throw LexerException.EOFException(index)
        }
    }

    private fun BufferedInputStream.peek(): Int {
        this.mark(1)
        val byte = this.read()
        this.reset()
        if (byte != -1) {
            return byte
        }
        else {
            throw LexerException.EOFException(index)
        }
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

        var byte = this.peek()
        while (pred(byte)) {
            stream.write(this.readByte())
            byte = this.peek()
        }

        return stream.toByteArray()
    }

    private fun BufferedInputStream.expect(expected: Int): Int {
        val actual = this.readByte()
        if (actual == expected) {
            return actual
        }
        else {
            throw LexerException.LexingException(index, "Expected '$expected'; found '$actual'.")
        }
    }

    // ----------------
    // Inner classes.
    // ----------------

    interface Token
    interface PrimitiveToken : Token

    data class IntToken(val value: Int) : PrimitiveToken
    data class FloatToken(val value: Double) : PrimitiveToken
    data class StringToken(val value: String) : PrimitiveToken
    data class ListToken(val value: List<PrimitiveToken>) : Token

}

fun Lexer.Token.toList(): List<Lexer.Token> {
    return when (this) {
        is Lexer.ListToken -> this.value
        else -> listOf(this)
    }
}

sealed class LexerException(index: Int, msg: String) : Throwable("(At [$index]) $msg") {
    class LexingException(index: Int, msg: String) : LexerException(index, msg)
    class EOFException(index: Int) : LexerException(index, "Unexpected end of input")
}
