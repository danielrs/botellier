package org.botellier.command

import org.botellier.store.*
import org.botellier.value.*
import org.junit.Assert
import org.junit.Test

class CommandsTest {

    // Keys.

    @Test
    fun delCommand() {
        val store = Store()
        val del = DelCommand()

        store.transaction().begin {
            set("one", IntValue(1))
            set("two", IntValue(2))
            set("three", IntValue(3))
            set("four", IntValue(4))
        }

        del.key = CValue.Primitive.String("one")
        del.rest = CValue.Array.String(listOf(
                CValue.Primitive.String("two"),
                CValue.Primitive.String("three")
        ))
        Assert.assertEquals(StringValue("OK"), del.execute(store))
        Assert.assertEquals(1, store.size)
    }

    @Test
    fun existsCommand() {
        val store = Store()
        val exists = ExistsCommand()

        store.transaction().begin {
            set("one", IntValue(1))
            set("two", IntValue(2))
            set("three", IntValue(3))
        }

        exists.key = CValue.Primitive.String("one")
        exists.rest = CValue.Array.String(listOf(
                CValue.Primitive.String("two"),
                CValue.Primitive.String("three"),
                CValue.Primitive.String("not key"),
                CValue.Primitive.String("not there neither")
        ))
        Assert.assertEquals(IntValue(3), exists.execute(store))
    }

    @Test
    fun keysCommand() {
        val store = Store()
        val keys = KeysCommand()

        store.transaction().begin {
            set("one", IntValue(1))
            set("prone", StringValue("yes"))
            set("two", IntValue(2))
        }

        keys.pattern = CValue.Primitive.String(".*ne")
        Assert.assertEquals(IntValue(2), (keys.execute(store) as ListValue).size)
    }

    @Test
    fun renameCommand() {
        val store = Store()
        val rename = RenameCommand()

        store.transaction().begin {
            set("one", IntValue(1))
        }

        rename.key = CValue.Primitive.String("one")
        rename.newkey = CValue.Primitive.String("uno")
        Assert.assertEquals(StringValue("OK"), rename.execute(store))
        Assert.assertEquals(NilValue(), store.get("one"))
        Assert.assertEquals(IntValue(1), store.get("uno"))

        rename.key = rename.newkey
        Assert.assertEquals(StringValue("OK"), rename.execute(store))
        Assert.assertEquals(IntValue(1), store.get("uno"))
    }

    @Test
    fun typeCommand() {
        val store = Store()
        val type = TypeCommand()

        store.transaction().begin {
            set("one", IntValue(1))
        }

        type.key = CValue.Primitive.String("one")
        Assert.assertEquals(StringValue("IntValue"), type.execute(store))
        type.key = CValue.Primitive.String("not key")
        Assert.assertEquals(StringValue("NilValue"), type.execute(store))
    }

    // Lists.

    @Test
    fun lindexCommand() {
        val store = Store()
        val lindex = LIndexCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        lindex.key = CValue.Primitive.String("list")
        lindex.index = CValue.Primitive.Int(2)
        Assert.assertEquals(IntValue(3), lindex.execute(store))
    }

    @Test
    fun linsertCommand() {
        val store = Store()
        val linsert = LInsertCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        linsert.key = CValue.Primitive.String("list")
        linsert.position = CValue.Primitive.String("BEFORE")
        linsert.pivot = CValue.Primitive.Int(1)
        linsert.value = CValue.Primitive.Int(0)
        Assert.assertEquals(IntValue(4), linsert.execute(store))
        Assert.assertEquals(IntValue(0), (store.get("list") as ListValue).unwrap().get(0))

        linsert.position = CValue.Primitive.String("AFTER")
        linsert.value = CValue.Primitive.Float(1.5)
        Assert.assertEquals(IntValue(5), linsert.execute(store))
        Assert.assertEquals(FloatValue(1.5), (store.get("list") as ListValue).unwrap().get(2))
    }

    @Test
    fun llenCommand() {
        val store = Store()
        val llen = LLenCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        llen.key = CValue.Primitive.String("list")
        Assert.assertEquals(IntValue(3), llen.execute(store))
        llen.key = CValue.Primitive.String("not key")
        Assert.assertEquals(IntValue(0), llen.execute(store))
    }

    @Test
    fun lpopCommand() {
        val store = Store()
        val lpop = LPopCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        lpop.key = CValue.Primitive.String("list")
        Assert.assertEquals(IntValue(1), lpop.execute(store))
        Assert.assertEquals(IntValue(2), lpop.execute(store))
        Assert.assertEquals(IntValue(3), lpop.execute(store))
        Assert.assertEquals(NilValue(), lpop.execute(store))
    }

    @Test
    fun lpushCommand() {
        val store = Store()
        val lpush = LPushCommand()
        lpush.key = CValue.Primitive.String("new-list")
        lpush.value = CValue.Primitive.Int(1)
        lpush.rest = CValue.Array.Any(listOf(CValue.Primitive.Float(2.0), CValue.Primitive.String("three")))
        Assert.assertEquals(IntValue(3), lpush.execute(store))
        Assert.assertEquals(IntValue(1), (store.get("new-list") as ListValue).unwrap().last())
    }

    @Test
    fun lrangeCommand() {
        val store = Store()
        val lrange = LRangeCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        lrange.key = CValue.Primitive.String("list")
        lrange.start = CValue.Primitive.Int(-2)
        lrange.stop = CValue.Primitive.Int(2)
        Assert.assertEquals(listOf(2, 3), (lrange.execute(store) as ListValue).toList().map { (it as IntValue).unwrap() })
    }

    @Test
    fun lremCommand() {
        val store = Store()
        val lrem = LRemCommand()
        lrem.key = CValue.Primitive.String("list")

        store.transaction().begin {
            set("list", listOf(1, 1, 2, 2).map { it.toValue() }.toValue())
        }

        lrem.count = CValue.Primitive.Int(-2)
        lrem.value = CValue.Primitive.Int(1)
        Assert.assertEquals(IntValue(2), lrem.execute(store))
        Assert.assertEquals(listOf(2, 2), (store.get("list") as ListValue).toList().map { (it as IntValue).unwrap() })

        store.transaction().begin {
            set("list", listOf(1, 1, 2, 2).map { it.toValue() }.toValue())
        }

        lrem.count = CValue.Primitive.Int(2)
        lrem.value = CValue.Primitive.Int(2)
        Assert.assertEquals(IntValue(2), lrem.execute(store))
        Assert.assertEquals(listOf(1, 1), (store.get("list") as ListValue).toList().map { (it as IntValue).unwrap() })
    }

    @Test
    fun lsetCommand() {
        val store = Store()
        val lset = LSetCommand()

        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }

        lset.key = CValue.Primitive.String("list")
        lset.index = CValue.Primitive.Int(2)
        lset.value = CValue.Primitive.Int(3)
        Assert.assertEquals(StringValue("OK"), lset.execute(store))
        Assert.assertEquals(IntValue(3), (store.get("list") as ListValue).unwrap().get(2))
    }

    @Test
    fun ltrimCommand() {
        val store = Store()
        val ltrim = LTrimCommand()
        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }
        ltrim.key = CValue.Primitive.String("list")
        ltrim.start = CValue.Primitive.Int(0)
        ltrim.stop = CValue.Primitive.Int(1)
        Assert.assertEquals(StringValue("OK"), ltrim.execute(store))
        Assert.assertEquals(1, store.size)
        ltrim.start = CValue.Primitive.Int(1)
        ltrim.stop = CValue.Primitive.Int(0)
        Assert.assertEquals(StringValue("OK"), ltrim.execute(store))
        Assert.assertEquals(0, store.size)
    }

    @Test
    fun rpopCommand() {
        val store = Store()
        val rpop = RPopCommand()
        store.transaction().begin {
            set("list", listOf(1, 2, 3).map { it.toValue() }.toValue())
        }
        rpop.key = CValue.Primitive.String("list")
        Assert.assertEquals(IntValue(3), rpop.execute(store))
        Assert.assertEquals(IntValue(2), rpop.execute(store))
        Assert.assertEquals(IntValue(1), rpop.execute(store))
        Assert.assertEquals(NilValue(), rpop.execute(store))
    }

    @Test
    fun rpushCommand() {
        val store = Store()
        val rpush = RPushCommand()
        rpush.key = CValue.Primitive.String("list")
        rpush.value = CValue.Primitive.Int(1)
        rpush.rest = CValue.Array.Any(listOf(CValue.Primitive.Float(2.0), CValue.Primitive.String("three")))
        Assert.assertEquals(IntValue(3), rpush.execute(store))
        Assert.assertEquals(IntValue(1), (store.get("list") as ListValue).unwrap().first())
    }

    // Strings.

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
        store.transaction().begin {
            set("key", StringValue("Hello, world!"))
        }
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
        store.transaction().begin {
            set("one", IntValue(1))
        }
        mget.key = CValue.Primitive.String("zero")
        mget.rest = CValue.Array.String(listOf("one", "two").map(CValue.Primitive::String))

        val list = (mget.execute(store) as ListValue).unwrap()
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
        store.transaction().begin {
            set("message", StringValue("Hello, world!"))
        }
        strlen.key = CValue.Primitive.String("message")
        Assert.assertEquals(IntValue(13), strlen.execute(store))
    }
}
