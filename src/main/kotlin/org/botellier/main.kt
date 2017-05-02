package org.botellier

import org.botellier.store.*
import org.botellier.serializer.toByteArray
import org.botellier.serializer.toJson

fun main(args: Array<String>) {
    val store = Store()

    store.set("one", 1.toValue())
    store.set("two", 2.toValue())
    store.set("three", 3.0.toValue())
    store.set("name", "John".toValue())
    store.set("numbers", listOf(4, 5, 6).map(Int::toValue).toValue())
    store.set("strings", listOf("Four", "Five", "Six").map(String::toValue).toValue())
    store.set("used", setOf("shoes", "pants").toValue())

    val mixedList = listOf(
            IntValue(1),
            FloatValue(2.0),
            StringValue("Three")
    )

    println("Store: ${store.toJson()}")

    val value = store
    val bytes = value.toByteArray()
    println("CValue: ${String(bytes)}")
}