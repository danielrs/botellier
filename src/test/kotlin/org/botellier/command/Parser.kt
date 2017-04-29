package org.botellier.command

import org.junit.Assert
import org.junit.Test

class ParserTest {

    @Test
    fun parseInt() {
        val message = "$3\r\n100\r\n"
        var value = 0

        parse(message) {
            value = int()
        }

        Assert.assertEquals(value, 100)
    }

    @Test
    fun parseFloat() {
        val message = "$4\r\n10.1\r\n"
        var value = 0.0

        parse(message) {
            value = float()
        }

        Assert.assertEquals(value, 10.1, 0.001)
    }

    @Test
    fun parseString() {
        val message = "$3\r\none\r\n"
        var value = ""

        parse(message) {
            value = string()
        }

        Assert.assertEquals(value, "one")
    }

    @Test
    fun parseList() {
        val message = "$3\r\none\r\n$3\r\ntwo\r\n$5\r\nthree\r\n"
        val values = mutableListOf<String>()

        parse(message) {
            while (true) {
                try { values.add(string()) } catch(e: Throwable) { break }
            }
        }

        Assert.assertEquals(values, listOf("one", "two", "three"))
    }

    @Test
    fun parseMixed() {
        val message = "$3\r\n100\r\n$7\r\n200.200\r\n$13\r\nthree-hundred\r\n"
        var int = 0
        var float = 0.0
        var string = ""

        parse(message) {
            int = int()
            float = float()
            string = string()
        }

        Assert.assertEquals(100, int)
        Assert.assertEquals(200.200, float, 0.001)
        Assert.assertEquals("three-hundred", string)
    }
}
