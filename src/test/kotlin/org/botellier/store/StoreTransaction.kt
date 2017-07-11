package org.botellier.store

import org.junit.Test
import org.junit.Assert

class StoreTransactionTest {
    @Test
    fun cachingDuringTransaction() {
        val store = Store()
        val transaction = store.transaction()
        transaction.begin {
            set("one", IntValue(1))
            Assert.assertEquals(IntValue(1), get("one"))
            Assert.assertEquals(NilValue(), store.get("one"))
        }
        Assert.assertEquals(IntValue(1), store.get("one"))
    }

    @Test
    fun updatingExistingValues() {
        val store = Store()
        val transaction = store.transaction()

        transaction.begin {
            set("one", IntValue(1))
            set("two", IntValue(2))
            set("three", IntValue(3))
        }

        transaction.begin {
            update<IntValue>("one") { IntValue(it.unwrap() * 10) }
            update<IntValue>("two") { IntValue(it.unwrap() * 10) }
            update<IntValue>("three") { IntValue(it.unwrap() * 10) }
        }

        Assert.assertEquals(IntValue(10), store.get("one"))
        Assert.assertEquals(IntValue(20), store.get("two"))
        Assert.assertEquals(IntValue(30), store.get("three"))
    }

    @Test
    fun updatingUnexistingValues() {
        val store = Store()
        val transaction = store.transaction()

        transaction.begin {
            mupdate<IntValue>("one") { IntValue((it?.unwrap() ?: 9) * 10) }
        }

        Assert.assertEquals(IntValue(90), store.get("one"))
    }
}
