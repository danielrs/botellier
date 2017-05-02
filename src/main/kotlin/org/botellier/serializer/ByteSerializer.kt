package org.botellier.serializer

import org.botellier.store.*
import java.io.ByteArrayOutputStream

private val CR: Byte = '\r'.toByte()
private val LF: Byte = '\n'.toByte()
private val NEWLINE: ByteArray = byteArrayOf(CR, LF)

class ByteSerializer(override val value: StoreType) : Serializer {
    override fun serialize(): ByteArray {
        val bos = ByteArrayOutputStream()
        render(bos, value)
        return bos.toByteArray()
    }

    private fun render(bos: ByteArrayOutputStream, value: StoreType) {
        when (value) {
            is IntValue -> renderInt(bos, value)
            is FloatValue -> renderFloat(bos, value)
            is StringValue -> renderString(bos, value)
            is ListValue -> renderList(bos, value)
            is SetValue -> renderSet(bos, value)
            is MapValue -> renderMap(bos, value)
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
        bos.write('*'.toInt())
        bos.write(list.size.toString().toByteArray())
        bos.write(NEWLINE)
        for (value in list) {
            render(bos, value)
        }
    }

    private fun renderSet(bos: ByteArrayOutputStream, set: SetValue) {
        bos.write('&'.toInt())
        bos.write(set.size.toString().toByteArray())
        bos.write(NEWLINE)
        for (value in set) {
            render(bos, StringValue(value))
        }
    }

    private fun renderMap(bos: ByteArrayOutputStream, map: MapValue) {
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
fun StoreType.toByteArray(): ByteArray = ByteSerializer(this).serialize()
