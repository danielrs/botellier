package org.botellier.node

/**
 * This class helps to get paths at the znode
 * service.
 */
class Path {
    companion object {
        const val leader = "/leader"
        const val leader_name = "$leader/name"
        val leader_version = "$leader/version"

        const val synced = "/synced"
        const val synced_name = "$synced/name"
        const val synced_version = "$synced/version"

        const val replicas = "/replicas"
        fun replicaPath(name: String): String = "$replicas/$name"

        const val changes = "/changes"
        fun changePath(name: String): String = "$changes/$name"
    }
}
