package org.botellier.node

import mu.KLogging
import org.apache.zookeeper.*
import org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE
import java.util.*

class Node (zooServers: String) : Watcher {
    companion object : KLogging() // sets up logging.

    val id = Integer.toHexString(Random().nextInt())
    val zk = ZooKeeper(zooServers, 4000, this)
    var type = NodeType.REPLICA
        private set

    private var version = 0
    private var totalReplicas = 0 // replicas that are not leader or synced follower.

    // ----------------
    // Bootstrap.
    // ----------------

    /**
     * Creates all the required persistent directories if they
     * don't exist.
     */
    fun bootstrap() {
        // Leader and synced replica.
        createDirectory(Path.leader)
        createDirectory(Path.synced)

        // Multi directories.
        createDirectory(Path.replicas)
        createDirectory(Path.changes)
    }

    private fun createDirectory(path: String) {
        zk.create(path, null, OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createDirectoryCb, path)
    }

    private val createDirectoryCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        val path = ctx as String
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                createDirectory(path)
            }
            KeeperException.Code.OK -> {
                logger.info { "Directory created: $path" }
            }
            KeeperException.Code.NODEEXISTS -> {
                logger.warn { "Directory already exists: $path" }
            }
            else -> {
                logger.error {
                    "Error when creating $path: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    // ----------------
    // Registration.
    // ----------------

    fun register() {
        zk.create(Path.replicaPath(name()), version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, registerCb, null)
    }

    private val registerCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                register()
            }
            KeeperException.Code.OK -> {
                logger.info { "Node successfully registered: $path" }
            }
            KeeperException.Code.NODEEXISTS -> {
                logger.warn { "Node already registered: $path" }
            }
            else -> {
                logger.error {
                    "Error when registering $path: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun updateVersion() {
        zk.setData(Path.replicaPath(name()), version.toString().toByteArray(), version, updateVersionCb, null)
    }

    private val updateVersionCb = AsyncCallback.StatCallback { rc, path, ctx, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                updateVersion()
            }
            KeeperException.Code.OK -> {
                logger.info { "Node version updated: $path" }
            }
            else -> {
                logger.error {
                    "Error updating versino of $path: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    // ----------------
    // Synced replica election.
    // ----------------
    // Nodes usually try to run for synced replica first. After that, they try to take the leader
    // role if the spot is available.

    fun runForSynced() {
        ensureReplica {
            zk.getData(Path.synced_name, false, runForSyncedCb, null)
        }
    }

    private val runForSyncedCb = AsyncCallback.DataCallback { rc, path, ctx, data, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                runForSynced()
            }
            KeeperException.Code.OK -> {
                if (String(data) == name()) {
                    type = NodeType.SYNCHRONIZED_REPLICA
                    runForLeader()
                } else {
                    type = NodeType.REPLICA
                    watchSynced()
                }
            }
            KeeperException.Code.NONODE -> {
                checkCandidacy()
            }
            else -> {
                logger.error {
                    "Error running for synced replica: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun watchSynced() {
        ensureReplica {
            zk.exists(Path.synced_name, watchSyncedWatcher, watchSyncedCb, null)
        }
    }

    private val watchSyncedWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert(Path.synced_name.equals(it.path))
                runForSynced()
            }
        }
    }

    private val watchSyncedCb = AsyncCallback.StatCallback { rc, path, ctx, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                watchSynced()
            }
            KeeperException.Code.OK -> {
                if (stat == null) {
                    runForSynced()
                }
            }
            else -> {
                logger.error { "Error checking existence of $path: ${KeeperException.create(ecode)}" }
            }
        }
    }

    private fun checkCandidacy() {
        ensureReplica {
            zk.getChildren(Path.replicas, false, checkCandidacyCb, null)
        }
    }

    private val checkCandidacyCb = AsyncCallback.ChildrenCallback { rc, path, ctx, children ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                checkCandidacy()
            }
            KeeperException.Code.OK -> {
                if (bestCandidate(children) == name()) {
                    takeReplica()
                } else {
                    runForSynced()
                }
            }
            else -> {
                logger.error {
                    "Error getting candidates: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun bestCandidate(candidates: List<String>): String? {
        return candidates.map {
            val version = String(getData(Path.replicaPath(it)) ?: byteArrayOf()).toIntOrNull() ?: -1
            Pair(it, version)
        }.maxBy { it.second }?.first
    }

    private fun takeReplica() {
        ensureReplica {
            zk.multi(listOf(
                    Op.delete(Path.replicaPath(name()), -1),
                    Op.create(Path.synced_name, name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL),
                    Op.create(Path.synced_version, version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
            ), takeReplicaCb, null)
        }
    }

    private val takeReplicaCb = AsyncCallback.MultiCallback { rc, path, ctx, opResults ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                takeReplica()
            }
            KeeperException.Code.OK -> {
                type = NodeType.SYNCHRONIZED_REPLICA
                logger.info { "${name()} is now the synced replica." }
                runForLeader()
            }
            KeeperException.Code.NODEEXISTS -> {
                runForSynced()
            }
            else -> {
                logger.error { "Error taking role of synced replica on ${name()}: ${KeeperException.create(ecode)}" }
            }
        }
    }

    // ----------------
    // Leader election.
    // ----------------

    fun runForLeader() {
        ensureSynced {
            zk.getData(Path.leader_name, false, runForLeaderCb, null)
        }
    }

    private val runForLeaderCb = AsyncCallback.DataCallback { rc, path, ctx, data, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                runForLeader()
            }
            KeeperException.Code.OK -> {
                if (String(data) == name()) {
                    type = NodeType.LEADER
                } else {
                    watchLeader()
                }
            }
            KeeperException.Code.NONODE -> {
                takeLeader()
            }
            else -> {
                "Error getting data of $path: ${KeeperException.create(ecode)}"
            }
        }
    }

    private fun watchLeader() {
        ensureSynced {
            zk.exists(Path.leader_name, watchLeaderWatch, watchLeaderCb, null)
        }
    }

    private val watchLeaderWatch = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert(Path.leader_name.equals(it.path))
                runForLeader()
            }
        }
    }

    private val watchLeaderCb = AsyncCallback.StatCallback { rc, path, ctx, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                watchLeader()
            }
            KeeperException.Code.OK -> {
                if (stat == null) {
                    runForLeader()
                }
            }
            else -> {
                logger.error {
                    "Error checking for leader existence: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun takeLeader() {
        ensureSynced {
            zk.multi(listOf(
                    Op.delete(Path.synced_name, -1),
                    Op.delete(Path.synced_version, -1),
                    Op.create(Path.leader_name, name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL),
                    Op.create(Path.leader_version, version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
            ), takeLeaderCb, null)
        }
    }

    private val takeLeaderCb = AsyncCallback.MultiCallback { rc, path, ctx, opResults ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                takeLeader()
            }
            KeeperException.Code.OK -> {
                type = NodeType.LEADER
                logger.info { "${name()} is now the leader." }
            }
            KeeperException.Code.NODEEXISTS -> {
                runForLeader()
            }
            else -> {
                logger.error { "Error taking role of leader on ${name()}: ${KeeperException.create(ecode)}" }
            }
        }
    }

    // ----------------
    // Replicas.
    // ----------------

    fun countReplicas() {
        zk.getChildren(Path.replicas, countReplicasWatcher, countReplicasCb, null)
    }

    private val countReplicasWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeChildrenChanged -> {
                assert(Path.replicas.equals(it.path))
                countReplicas()
            }
        }
    }

    private val countReplicasCb = AsyncCallback.ChildrenCallback { rc, path, ctx, children ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                countReplicas()
            }
            KeeperException.Code.OK -> {
                if (children == null) {
                    totalReplicas = 0
                } else {
                    totalReplicas = children.size
                }
                logger.info { "Total replicas is now: $totalReplicas"}
            }
            else -> {
                logger.error {
                    "Error counting replicas $path: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    // ----------------
    // Other methods.
    // ----------------

    /**
     * Gets the name of the node in the form of 'node-[ID]'
     * @return the name of the node.
     */
    fun name(): String = "node-$id"

    /**
     * Makes sure the current node is a replica
     * before running the supplied callback.
     */
    private fun ensureReplica(f: () -> Unit) {
        if (type == NodeType.REPLICA) {
            f()
        }
    }

    /**
     * Makes sure the current node is a synced replica
     * before running the supplied callback.
     */
    private fun ensureSynced(f: () -> Unit) {
        if (type == NodeType.SYNCHRONIZED_REPLICA) {
            f()
        } else if (type == NodeType.REPLICA) {
            runForSynced()
        }
    }

    // ----------------
    // Zookeeper helpers.
    // ----------------

    /**
     * Synchronous function to get node data if it exists.
     *
     */
    private fun getData(path: String): ByteArray? {
        while (true) {
            try {
                val data = zk.getData(path, false, null)
                return data
            } catch (e: KeeperException.NoNodeException) {
                break
            } catch (e: KeeperException.ConnectionLossException) {
                continue
            }
        }
        return null
    }

    // ----------------
    // Watcher.
    // ----------------

    override fun process(event: WatchedEvent?) {
        logger.info { "Event received on ${name()}: $event" }
    }

    // ----------------
    // Nested classes.
    // ----------------

    enum class NodeType {
        LEADER, SYNCHRONIZED_REPLICA, REPLICA
    }
}
