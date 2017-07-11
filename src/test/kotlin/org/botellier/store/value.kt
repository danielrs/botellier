package org.botellier.store

import org.junit.Assert
import org.junit.Test

class StoreValueTest {
    @Test
    fun builtinConversionsWork() {
        Assert.assertTrue(1.toValue() is IntValue)
        Assert.assertTrue(1.0f.toValue() is FloatValue)
        Assert.assertTrue(1.0.toValue() is FloatValue)
        Assert.assertTrue("".toValue() is StringValue)
        Assert.assertTrue(listOf(1, 2, 3).map(Int::toValue).toValue() is ListValue)
        Assert.assertTrue(setOf("one").toValue() is SetValue)
        Assert.assertTrue(mapOf("one" to 1.toValue()).toValue() is MapValue)
    }

    @Test
    fun primitivesComparisons() {
        val int = IntValue(1)
        val float = FloatValue(1.0)
        val string = StringValue("one")

        Assert.assertTrue(int < IntValue(2))
        Assert.assertTrue(int > IntValue(0))

        Assert.assertTrue(float < FloatValue(2.0))
        Assert.assertTrue(float > FloatValue(0.0))

        Assert.assertTrue(string < StringValue("two"))
        Assert.assertTrue(string > StringValue("abc"))
    }

    @Test
    fun listAndSetCloneNotSameAsOriginal() {
        val list = ListValue(listOf(1, 2).map(Int::toValue))
        val set = SetValue(setOf("one", "two"))

        val listClone = list.copy { it.rpush(3.toValue()) }
        val setClone = set.copy { it.add("three") }

        Assert.assertNotEquals(list.size, listClone.size)
        Assert.assertNotEquals(set.size, setClone.size)
    }

    @Test
    fun modifyingListClone() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue()
        val clone = list.copy { it.lpop() }

        Assert.assertEquals(list.size, 3)
        Assert.assertEquals(clone.size, 2)
    }

    @Test
    fun modifyingSetClone() {
        val set = setOf("one", "two", "three").toValue()
        val clone = set.copy { it.remove("one") }

        Assert.assertEquals(set.size, 3)
        Assert.assertEquals(clone.size, 2)
    }

    @Test
    fun iteratingList() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue()
        var res = 0
        for (value in list) {
            when (value) {
                is IntValue -> res += value.unwrap()
            }
        }
        Assert.assertEquals(res, 6)
    }

    @Test
    fun iteratingSet() {
        val set = setOf("one", "two").toValue()
        var res = ""
        for (value in set) {
            res += value
        }
        Assert.assertEquals(res, "onetwo")
    }

    @Test
    fun iteratingMap() {
        val map = mapOf("one" to 1, "two" to 2).mapValues { it.value.toValue() }.toValue()
        var res = 0
        for ((_, value) in map) {
            when (value) {
                is IntValue -> res += value.unwrap()
            }
        }
        Assert.assertEquals(res, 3)
    }

    @Test
    fun settingAndGettingList() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue().copy {
            it[1] = 2.0.toValue()
            it[2] = "3".toValue()
        }

        Assert.assertEquals((list.unwrap().get(0) as IntValue).unwrap(), 1)
        Assert.assertEquals((list.unwrap().get(1) as FloatValue).unwrap(), 2.0, 0.001)
        Assert.assertEquals((list.unwrap().get(2) as StringValue).unwrap(), "3")
    }

    @Test
    fun removingFromList() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9).map(Int::toValue).toValue().copy {
            it.removeAt(0)
            it.remove(listOf(0, 0, 1, 1, 2, 3, 4, 5, 6))
        }
        Assert.assertEquals(1, list.size)
        Assert.assertEquals(IntValue(9), list.unwrap().first())
    }

    @Test
    fun slicingList() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue()
        Assert.assertEquals(list.unwrap().slice(0, 2).toList().map{ (it as IntValue).unwrap() }, listOf(1, 2, 3))
        Assert.assertEquals(list.unwrap().slice(0, 1).toList().map{ (it as IntValue).unwrap() }, listOf(1, 2))
        Assert.assertEquals(list.unwrap().slice(-3, 2).toList().map{ (it as IntValue).unwrap() }, listOf(1, 2, 3))
    }
}