package org.botellier.command

import org.botellier.store.*
import org.junit.Assert
import org.junit.Test

class CommandsTest {

    @Test
    fun lpushCommand() {
        val store = Store()
        val lpush = LPushCommand()
        lpush.key = CValue.Primitive.String("key")
        lpush.value = CValue.Primitive.Int(1)
        lpush.rest = CValue.Array.Any(listOf(CValue.Primitive.Float(2.0), CValue.Primitive.String("three")))
        Assert.assertEquals(IntValue(3), lpush.execute(store))
        Assert.assertEquals(1, store.size)
    }

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
    fun mgetCommand() {
        val store = Store()
        val mget = MGetCommand()
        store.set("key1", IntValue(1))
        mget.key = CValue.Primitive.String("key0")
        mget.rest = CValue.Array.String(listOf("key1", "key2").map(CValue.Primitive::String))

        val list = mget.execute(store) as ListValue
        Assert.assertEquals(NilValue(), list.get(0))
        Assert.assertEquals(IntValue(1), list.get(1))
        Assert.assertEquals(NilValue(), list.get(2))
    }

    @Test
    fun msetCommand() {
        val store = Store()
        val mset = MSetCommand()
        mset.key = CValue.Primitive.String("key0")
        mset.value = CValue.Primitive.Int(0)
        mset.rest = CValue.Array.Pair(listOf(
                CValue.Pair("key1", CValue.Primitive.Float(1.0)),
                CValue.Pair("key2", CValue.Primitive.String("two"))
        ))
        Assert.assertEquals(StringValue("OK"), mset.execute(store))
        Assert.assertEquals(IntValue(0), store.get("key0"))
        Assert.assertEquals(FloatValue(1.0), store.get("key1"))
        Assert.assertEquals(StringValue("two"), store.get("key2"))
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
