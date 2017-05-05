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
                                }
                            }
                        }
                        return command
                    }

                }
            }

            throw UnknownCommandException(firstToken.toString())
        }
    }

   // Exceptions.
   class UnknownCommandException(command: String)
       : Throwable("Command not recognized: $command")
}
