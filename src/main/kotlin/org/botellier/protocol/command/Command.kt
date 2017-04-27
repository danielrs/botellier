package org.botellier.protocol.command

/**
 * General commands
 */

enum class CommandType {
    SERVER,
    STORE,
}

interface Command {
    abstract val type: CommandType
    abstract val name: String
}

class ServerCommand(override val name: String) : Command {
    override val type = CommandType.SERVER
}

class StoreCommand(override val name: String) : Command {
    override val type = CommandType.STORE
}

/**
 * Specific commands
 */
