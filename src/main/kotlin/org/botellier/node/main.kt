package org.botellier.node


fun main(args: Array<String>) {
    val node0 = Node("127.0.0.1:2181")
    val node1 = Node("127.0.0.1:2181")
    val node2 = Node("127.0.0.1:2181")
    val node3 = Node("127.0.0.1:2181")
    val node4 = Node("127.0.0.1:2181")

    node0.bootstrap()
    node1.bootstrap()
    node2.bootstrap()
    node3.bootstrap()
    node4.bootstrap()

    node0.register()
    node1.register()
    node2.register()
    node3.register()
    node4.register()

    node0.countReplicas()
    node1.countReplicas()
    node2.countReplicas()
    node3.countReplicas()
    node4.countReplicas()

    Thread.sleep(30000)
}