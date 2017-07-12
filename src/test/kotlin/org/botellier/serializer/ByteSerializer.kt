package org.botellier.serializer

import org.botellier.value.*
import org.junit.Assert
import org.junit.Test

class ByteSerializerTest {
    @Test
    fun renderInt() {
        val value = IntValue(1)
        Assert.assertArrayEquals(value.toByteArray(), ":1\r\n".toByteArray())
    }

    @Test
    fun renderFloat() {
        val value = FloatValue(2.0)
        Assert.assertArrayEquals(value.toByteArray(), ";2.0\r\n".toByteArray())
    }

    @Test
    fun renderString() {
        val value = StringValue("Hello, World!")
        Assert.assertArrayEquals(value.toByteArray(), "$13\r\nHello, World!\r\n".toByteArray())
    }

    @Test
    fun renderEmptyString() {
        val value = StringValue("")
        Assert.assertArrayEquals(value.toByteArray(), "$0\r\n\r\n".toByteArray())
    }

    @Test
    fun renderList() {
        val value = ListValue(listOf(IntValue(1), FloatValue(2.0), StringValue("three")))
        Assert.assertArrayEquals(
                value.toByteArray(),
                "*3\r\n:1\r\n;2.0\r\n$5\r\nthree\r\n".toByteArray()
        )
    }

    @Test
    fun renderEmptyList() {
        val value = ListValue()
        Assert.assertArrayEquals(value.toByteArray(), "*0\r\n".toByteArray())
    }

    @Test
    fun renderSet() {
        val value = SetValue(listOf("one", "two", "three").toSet())
        Assert.assertArrayEquals(
                value.toByteArray(),
                "&3\r\n$3\r\none\r\n$3\r\ntwo\r\n$5\r\nthree\r\n".toByteArray()
        )
    }

    @Test
    fun renderEmptySet() {
        val value = SetValue()
        Assert.assertArrayEquals(value.toByteArray(), "&0\r\n".toByteArray())
    }

    @Test
    fun renderMap() {
        val value = MapValue(mapOf("one" to IntValue(1), "two" to FloatValue(2.0), "three" to StringValue("three")))
        Assert.assertArrayEquals(
                value.toByteArray(),
                "#3\r\n$3\r\none\r\n:1\r\n$3\r\ntwo\r\n;2.0\r\n$5\r\nthree\r\n$5\r\nthree\r\n".toByteArray()
        )
    }

    @Test
    fun renderEmptyMap() {
        val value = MapValue()
        Assert.assertArrayEquals(value.toByteArray(), "#0\r\n".toByteArray())
    }
}