package org.botellier.protocol.command

import kotlin.reflect.full.createInstance

// from: https://redis.io/commands

val COMMANDS = arrayOf(
        AppendCommand::class,
        DecrCommand::class,
        DecrbyCommand::class,
        GetCommand::class,
        IncrCommand::class,
        IncrbyCommand::class,
        IncrbyfloatCommand::class,
        SetCommand::class,
        StrlenCommand::class
).map { it.createInstance().name to it }.toMap()

/**
 * Strings
 */

@WithCommand("APPEND")
class AppendCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue
}

@WithCommand("DECR")
class DecrCommand : Command() {
    @field:Parameter(0)
    var key = stringValue
}

@WithCommand("DECRBY")
class DecrbyCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var decrement = intValue
}

@WithCommand("GET")
class GetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue
}

@WithCommand("INCR")
class IncrCommand : Command() {
    @field:Parameter(0)
    var key = stringValue
}

@WithCommand("INCRBY")
class IncrbyCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = intValue
}

@WithCommand("INCRBYFLOAT")
class IncrbyfloatCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var increment = floatValue
}

@WithCommand("SET")
class SetCommand : Command() {
    @field:Parameter(0)
    var key = stringValue

    @field:Parameter(1)
    var value = anyValue
}

@WithCommand("STRLEN")
class StrlenCommand : Command() {
    @field:Parameter(0)
    var key = stringValue
}
