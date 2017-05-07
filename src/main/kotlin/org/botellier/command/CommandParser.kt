package org.botellier.command

import kotlin.reflect.full.createInstance

class CommandParser {
    companion object Factory {
        fun parse(tokens: List<Lexer.Token>): Command {
            val firstToken = tokens.first()

            if (firstToken is Lexer.StringToken) {
                val commandClass = COMMANDS[firstToken.value.toUpperCase()]
                if (commandClass != null) {
                    val command = commandClass.createInstance()

                    if (command.parameters.isNotEmpty()) {
                        parse(tokens.drop(1)) {
                            for (p in command.parameters) {
                                when {
                                    p.isInt -> p.set(CValue.Primitive.Int(int()))
                                    p.isFloat -> p.set(CValue.Primitive.Float(float()))
                                    p.isString -> p.set(CValue.Primitive.String(string()))
                                    p.isAny -> p.set(CValue.primitive(any()))
                                    p.isIntArray -> {
                                        p.set(CValue.Array.Int(
                                                many(this::int).map(CValue.Primitive::Int)
                                        ))
                                    }
                                    p.isFloatArray -> {
                                        p.set(CValue.Array.Float(
                                                many(this::float).map(CValue.Primitive::Float)
                                        ))
                                    }
                                    p.isStringArray -> {
                                        p.set(CValue.Array.String(
                                                many { string() }.map(CValue.Primitive::String)
                                        ))
                                    }
                                    p.isPairArray -> {
                                        p.set(CValue.Array.Pair(
                                                many {
                                                    val key = string()
                                                    val value = any()
                                                    Pair(key, value)
                                                }.map { (key, value) ->
                                                    CValue.Pair(
                                                            CValue.Primitive.String(key),
                                                            CValue.primitive(value)
                                                    )
                                                }
                                        ))
                                    }
                                    p.isAnyArray -> {
                                        p.set(CValue.Array.Any(
                                                many(this::any).map { CValue.primitive(it) }
                                        ))
                                    }
                                }
                            }
                        }
                    }

                    return command
                }
            }

            throw UnknownCommandException(firstToken.toString())
        }
    }

   // Exceptions.
   class UnknownCommandException(command: String)
       : Throwable("Command not recognized: $command")
}
