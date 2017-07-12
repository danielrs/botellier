package org.botellier.store

import org.botellier.log.DeleteEntry
import org.botellier.log.Log
import org.botellier.log.SetEntry
import org.botellier.value.parseValue
import kotlin.system.exitProcess

/**
 * Store that can be persisted to disk by
 * using org.botellier.log.
 * @param root the basedir for the logs.
 */
class PersistentStore(root: String) : Store() {
    val log = Log(root)

    init {
        val transaction = transaction()

        for (entry in log) {
            when (entry) {
                is DeleteEntry -> {
                    transaction.begin {
                        delete(entry.key)
                    }
                }
                is SetEntry -> {
                    transaction.begin {
                        val value = parseValue(entry.after)
                        set(entry.key, value)
                    }
                }
            }
        }
    }
}
