package org.botellier

import org.botellier.protocol.toByteArray
import org.botellier.store.*
import org.botellier.storeprinter.toJson

fun main(args: Array<String>) {
    val store = Store()

    store.set("one", 1.toValue())
    store.set("two", 2.toValue())
    store.set("three", 3.0.toValue())
    store.set("name", "John".toValue())
    store.set("numbers", listOf(4, 5, 6).map(Int::toValue).toValue())
    store.set("strings", listOf("Four", "Five", "Six").map(String::toValue).toValue())
    store.set("used", setOf("shoes", "pants").toValue())
    store.set("hashes", mapOf("1" to "one", "2" to "two", "3" to "three").mapValues { it.value.toValue() }.toValue())

    val mixedList = listOf(
            IntValue(1),
            FloatValue(2.0),
            StringValue("Three"),
            listOf(IntValue(4), FloatValue(5.0), StringValue("Six")).toValue()
    )

    store.set("mixed", mixedList.toValue())

    println("Store: ${store.toJson()}")

    val value = store
    val bytes = value.toByteArray()

    println("Value: ${String(bytes)}")
    println("Bytes: $bytes")
}