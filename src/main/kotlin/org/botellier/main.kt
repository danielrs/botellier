package main

import org.botellier.store.*

fun main(args: Array<String>) {
    val map = MapValue()
    map.set("one", IntValue(1))
    map.set("two", IntValue(2))
    map.set("three", IntValue(3))
    map.set("others", ListValue(listOf(IntValue(4), StringValue("Five"))))
    map.set("id", IntValue(3423412))

    println("Map: $map")
}