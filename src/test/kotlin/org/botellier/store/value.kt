package org.botellier.store

import org.junit.Assert
import org.junit.Test

class StoreValueTest {
    @Test
    fun typesAreWhatIsExpected() {
        Assert.assertEquals(IntValue(1).type(), ValueType.INT)
        Assert.assertEquals(FloatValue(1.0).type(), ValueType.FLOAT)
        Assert.assertEquals(StringValue("").type(), ValueType.STRING)
        Assert.assertEquals(SetValue(setOf()).type(), ValueType.SET)
        Assert.assertEquals(ListValue(listOf()).type(), ValueType.LIST)
        Assert.assertEquals(MapValue(mapOf()).type(), ValueType.MAP)
    }

    @Test
    fun builtinConversionsWork() {
        Assert.assertEquals(1.toValue().type(), ValueType.INT)
        Assert.assertEquals(1.0f.toValue().type(), ValueType.FLOAT)
        Assert.assertEquals(1.0.toValue().type(), ValueType.FLOAT)
        Assert.assertEquals("".toValue().type(), ValueType.STRING)
        Assert.assertEquals(listOf(1, 2, 3).map(Int::toValue).toValue().type(), ValueType.LIST)
        Assert.assertEquals(setOf("one").toValue().type(), ValueType.SET)
        Assert.assertEquals(mapOf("one" to 1).mapValues { it.value.toValue() }.toValue().type(), ValueType.MAP)
    }

    @Test
    fun cloneNotSameAsOriginal() {
        val int = IntValue(1)
        val float = FloatValue(1.0)
        val string = StringValue("one")
        val list = ListValue(listOf(1, 2).map(Int::toValue))
        val set = SetValue(setOf("one", "two"))
        val map = MapValue(mapOf("one" to 1, "two" to 2).mapValues { it.value.toValue() })

        val intClone = int.clone()
        val floatClone = float.clone()
        val stringClone = string.clone()
        val listClone = list.clone()
        val setClone = set.clone()
        val mapClone = map.clone()

        intClone.value = 10
        floatClone.value = 10.0
        stringClone.value = "ten"
        listClone.rpush(3.toValue())
        setClone.set("three")
        mapClone.set("three", 3.toValue())

        Assert.assertNotEquals(int.value, intClone.value)
        Assert.assertNotEquals(float.value, floatClone.value)
        Assert.assertNotEquals(string.value, stringClone.value)
        Assert.assertNotEquals(list.size, listClone.size)
        Assert.assertNotEquals(set.size, setClone.size)
        Assert.assertNotEquals(map.size, mapClone.size)
    }

    @Test
    fun modifyingListClone() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue()
        val clone =list.clone()
        clone.lpop()

        Assert.assertEquals(list.size, 3)
        Assert.assertEquals(clone.size, 2)
    }

    @Test
    fun modifyingSetClone() {
        val set = setOf("one", "two", "three").toValue()
        val clone = set.clone()
        clone.unset("one")

        Assert.assertEquals(set.size, 3)
        Assert.assertEquals(clone.size, 2)
    }

    @Test
    fun modifyingMapClone() {
        val map = mapOf("one" to 1, "two" to 2, "three" to 3).mapValues { it.value.toValue() }.toValue()
        val clone = map.clone()
        clone.unset("one")

        Assert.assertEquals(map.size, 3)
        Assert.assertEquals(clone.size, 2)
    }

    @Test
    fun iteratingList() {
        val list = listOf(1, 2, 3).map(Int::toValue).toValue()
        var res = 0
        for (value in list) {
            when (value) {
                is IntValue -> res += value.value
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
                is IntValue -> res += value.value
            }
        }
        Assert.assertEquals(res, 3)
    }
}