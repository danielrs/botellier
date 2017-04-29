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
                                    p.isInt -> p.set(CValue.primitive(int()))
                                    p.isFloat -> p.set(CValue.primitive(float()))
                                    p.isString -> p.set(CValue.primitive(string()))
                                    p.isAny -> p.set(CValue.primitive(any()))
                                }
                            }
                        }
                        return command
                    }

                }
            }

            throw InvalidCommandException(tokens)
        }
    }

   // Exceptions.
   class InvalidCommandException(tokens: List<Lexer.Token>)
       : Throwable("Command not recognized from given tokens: $tokens.")
}
