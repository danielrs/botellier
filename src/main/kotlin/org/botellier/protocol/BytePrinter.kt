package org.botellier.protocol

import org.botellier.store.*
import org.botellier.storeprinter.Printer
import java.io.ByteArrayOutputStream

private val CR: Byte = '\r'.toByte()
private val LF: Byte = '\n'.toByte()
private val NEWLINE: ByteArray = byteArrayOf(CR, LF)

class BytePrinter(override val value: StoreValue) : Printer {
    fun printBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        render(bos, value)
        return bos.toByteArray()
    }

    override fun print(): String = printBytes().toString()

    private fun render(bos: ByteArrayOutputStream, value: StoreValue) {
        when (value) {
            is IntValue -> renderInt(bos, value)
            is FloatValue -> renderFloat(bos, value)
            is StringValue -> renderString(bos, value)
            is ListValue -> renderList(bos, value)
            is SetValue -> renderSet(bos, value)
            is MapValue -> renderMap(bos, value.toMap())
        }
    }

    private fun renderInt(bos: ByteArrayOutputStream, value: IntValue) {
        val bytes = value.toString().toByteArray()
        bos.write(':'.toInt())
        bos.write(bytes)
        bos.write(NEWLINE)
    }

    private fun renderFloat(bos: ByteArrayOutputStream, value: FloatValue) {
        val bytes = value.toString().toByteArray()
        bos.write(';'.toInt())
        bos.write(bytes)
        bos.write(NEWLINE)
    }

    private fun renderString(bos: ByteArrayOutputStream, value: StringValue) {
        val bytes = value.value.toByteArray()
        bos.write('$'.toInt())
        bos.write(bytes.size.toString().toByteArray())
        bos.write(NEWLINE)
        bos.write(bytes)
        bos.write(NEWLINE)
    }

    private fun renderList(bos: ByteArrayOutputStream, list: ListValue) {
        renderSequence(bos, list, list.size)
    }

    private fun renderSet(bos: ByteArrayOutputStream, set: SetValue) {
        renderSequence(bos, set.map(String::toValue), set.size)
    }

    private fun renderSequence(bos: ByteArrayOutputStream, iterable: Iterable<StoreValue>, size: Int) {
        bos.write('*'.toInt())
        bos.write(size.toString().toByteArray())
        bos.write(NEWLINE)
        for (value in iterable) {
            render(bos, value)
        }
    }

    private fun renderMap(bos: ByteArrayOutputStream, map: Map<String, StoreValue>) {
        bos.write('#'.toInt())
        bos.write(map.size.toString().toByteArray())
        bos.write(NEWLINE)
        for ((key, value) in map) {
            render(bos, StringValue(key))
            render(bos, value)
        }
    }
}

// Extensions.
fun StoreValue.toByteArray(): ByteArray = BytePrinter(this).printBytes()
