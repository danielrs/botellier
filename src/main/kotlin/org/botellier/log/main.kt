package org.botellier.log

import java.io.DataInputStream

/**
 * Created by danielrs on 7/2/17.
 */
fun main(args: Array<String>) {
    val seg = Segment("./run", 0)

    seg.append(45, "45", "45".toByteArray())
    seg.delete(46, "46")

    for (entry in seg) {
        println(entry)
    }
}