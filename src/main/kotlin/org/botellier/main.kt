package main

import org.botellier.Store
import org.botellier.store.*

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

    println("Map contents:")
    for ((_, value) in store) {
        when (value) {
            is IntValue -> println("Integer: $value")
            is FloatValue -> println("Float: $value")
            is StringValue -> println("String: $value")
            is ListValue -> println("List: $value")
            is SetValue -> println("Set: $value")
            is MapValue -> println("Map: $value")
        }
    }

    println("Store: $store")
}