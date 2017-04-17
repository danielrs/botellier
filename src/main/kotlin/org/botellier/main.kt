package main

import org.botellier.Store
import org.botellier.store.*

fun main(args: Array<String>) {
    val store = Store()
    val map = MapValue()

    map.set("one", 1.toValue())
    map.set("two", 2.toValue())
    map.set("three", 3.0.toValue())
    map.set("name", "John".toValue())
    map.set("numbers", listOf(4, 5, 6).map(Int::toValue).toValue())
    map.set("strings", listOf("Four", "Five", "Six").map(String::toValue).toValue())
    map.set("used", setOf("shoes", "pants").toValue())
    map.set("hashes", mapOf("1" to "one", "2" to "two", "3" to "three").mapValues { it.value.toValue() }.toValue())

    println("Map contents:")
    for ((_, value) in map) {
        when (value) {
            is IntValue -> println("Integer: $value")
            is FloatValue -> println("Float: $value")
            is StringValue -> println("String: $value")
            is ListValue -> println("List: $value")
            is SetValue -> println("Set: $value")
            is MapValue -> println("Map: $value")
        }
    }

    store.set("data", map)
    println("Store: $store")

    val list = map.get("numbers") as ListValue
    for (item in list.toList().subList(0, 10)) {
        println(item)
    }

}