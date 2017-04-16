package main

import org.botellier.store.*

fun main(args: Array<String>) {
    val map = MapValue()
    map.set("one", 1.toValue())
    map.set("two", 2.toValue())
    map.set("three", 3.toValue())
    map.set("numbers", listOf(4, 5, 6).map(Int::toValue).toValue())
    map.set("strings", listOf("Four", "Five", "Six").map(String::toValue).toValue())
    map.set("used", setOf("shoes", "pants").toValue())

    println("Map: $map")
}