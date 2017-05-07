package org.botellier.command

import org.botellier.store.*
import org.junit.Assert
import org.junit.Test

class CommandsTest {

    @Test
    fun appendCommand() {
        val store = Store()
        val append = AppendCommand()
        append.key = CValue.Primitive.String("key")
        append.value = CValue.Primitive.Int(1)
        append.execute(store)
        append.value = CValue.Primitive.Float(2.0)
        append.execute(store)
        append.value = CValue.Primitive.String("three")
        Assert.assertEquals(IntValue(9), append.execute(store))
        Assert.assertEquals(StringValue("12.0three"), store.get("key"))
    }

    @Test
    fun decrCommand() {
        val store = Store()
        val decr = DecrCommand()
        decr.key = CValue.Primitive.String("key")
        Assert.assertEquals(IntValue(-1), decr.execute(store))
        Assert.assertEquals(IntValue(-1), store.get("key"))
    }

    @Test
    fun decrbyCommand() {
        val store = Store()
        val decrby = DecrbyCommand()
        decrby.key = CValue.Primitive.String("key")
        decrby.decrement = CValue.Primitive.Int(10)
        Assert.assertEquals(IntValue(-10), decrby.execute(store))
        Assert.assertEquals(IntValue(-10), store.get("key"))
    }

    @Test
    fun getCommand() {
        val store = Store()
        val get = GetCommand()
        store.set("key", StringValue("Hello, world!"))
        get.key = CValue.Primitive.String("key")
        Assert.assertEquals(StringValue("Hello, world!"), get.execute(store))
    }

    @Test
    fun incrCommand() {
        val store = Store()
        val incr = IncrCommand()
        incr.key = CValue.Primitive.String("key")
        Assert.assertEquals(IntValue(1), incr.execute(store))
        Assert.assertEquals(IntValue(1), store.get("key"))
    }

    @Test
    fun incrbyCommand() {
        val store = Store()
        val incrby = IncrbyCommand()
        incrby.key = CValue.Primitive.String("key")
        incrby.increment = CValue.Primitive.Int(10)
        Assert.assertEquals(IntValue(10), incrby.execute(store))
        Assert.assertEquals(IntValue(10), store.get("key"))
    }

    @Test
    fun incrbyfloatCommand() {
        val store = Store()
        val incrby = IncrbyfloatCommand()
        incrby.key = CValue.Primitive.String("key")
        incrby.increment = CValue.Primitive.Float(10.0)
        Assert.assertEquals(FloatValue(10.0), incrby.execute(store))
        Assert.assertEquals(FloatValue(10.0), store.get("key"))
    }

    @Test
    fun setCommand() {
        val store = Store()
        val set = SetCommand()
        set.key = CValue.Primitive.String("key")
        set.value = CValue.Primitive.String("Hello, world!")
        Assert.assertEquals(StringValue("OK"), set.execute(store))
        Assert.assertEquals(StringValue("Hello, world!"), store.get("key"))
    }

    @Test
    fun strlenCommand() {
        val store = Store()
        val strlen = StrlenCommand()
        store.set("key", StringValue("Hello, world!"))
        strlen.key = CValue.Primitive.String("key")
        Assert.assertEquals(IntValue(13), strlen.execute(store))
    }
}
