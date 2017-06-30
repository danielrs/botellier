package org.botellier.node

fun newNode(): Node {
    val node = Node("127.0.0.1:2181")
    node.bootstrap()
    node.register()
    node.countReplicas()
    node.runForLeader()
    return node
}

fun main(args: Array<String>) {
    for (i in 1..60) {
        newNode()
    }
    readLine()
}