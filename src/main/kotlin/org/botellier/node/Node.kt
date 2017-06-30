package org.botellier.node

import mu.KLogging
import org.apache.zookeeper.*
import org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE
import java.util.*

// Zookeeper path constants.
const val LEADER_PATH = "/leader"
const val SREPLICA_PATH = "/sreplica"
const val LEADER_WATCH_PATH = "$LEADER_PATH/name"
const val SREPLICA_WATCH_PATH = "$SREPLICA_PATH/name"
const val REPLICAS_PATH = "/replicas"
const val CHANGES_PATH = "/changes"

class Node (zooServers: String) : Watcher {
    companion object : KLogging() // sets up logging.

    val id = Integer.toHexString(Random().nextInt())
    val zk = ZooKeeper(zooServers, 4000, this)
    var type = NodeType.REPLICA
        private set

    private var version = 0
    private var totalReplicas = 0 // replicas that are not leader or synchronized follower.

    // ----------------
    // Bootstrap.
    // ----------------

    /**
     * Creates all the required persistent directories if they
     * don't exist.
     */
    fun bootstrap() {
        // Leader and synchronized replica.
        createDirectory(LEADER_PATH)
        createDirectory(SREPLICA_PATH)

        // Multi directories.
        createDirectory(REPLICAS_PATH)
        createDirectory(CHANGES_PATH)
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
        zk.create("$REPLICAS_PATH/${name()}", version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, registerCb, null)
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
        zk.setData("$REPLICAS_PATH/${name()}", version.toString().toByteArray(), version, updateVersionCb, null)
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
    // Synchronized replica election.
    // ----------------
    // Nodes usually try to run for synchronized replica first. After that, they try to take the leader
    // role if the spot is available.

    fun runForReplica() {
        ensureReplica {
            zk.getData(SREPLICA_WATCH_PATH, false, runForReplicaCb, null)
        }
    }

    private val runForReplicaCb = AsyncCallback.DataCallback { rc, path, ctx, data, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                runForReplica()
            }
            KeeperException.Code.OK -> {
                if (String(data) == name()) {
                    type = NodeType.SYNCHRONIZED_REPLICA
                    runForLeader()
                } else {
                    type = NodeType.REPLICA
                    watchReplica()
                }
            }
            KeeperException.Code.NONODE -> {
                checkCandidacy()
            }
            else -> {
                logger.error {
                    "Error running for synchronized replica: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun watchReplica() {
        ensureReplica {
            zk.exists(SREPLICA_WATCH_PATH, watchReplicaWatcher, watchReplicaCb, null)
        }
    }

    private val watchReplicaWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert(SREPLICA_WATCH_PATH.equals(it.path))
                runForReplica()
            }
        }
    }

    private val watchReplicaCb = AsyncCallback.StatCallback { rc, path, ctx, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                watchReplica()
            }
            KeeperException.Code.OK -> {
                if (stat == null) {
                    runForReplica()
                }
            }
            else -> {
                logger.error { "Error checking existence of $path: ${KeeperException.create(ecode)}" }
            }
        }
    }

    private fun checkCandidacy() {
        ensureReplica {
            zk.getChildren(REPLICAS_PATH, false, checkCandidacyCb, null)
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
                    runForReplica()
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
            val version = String(getData("$REPLICAS_PATH/$it") ?: byteArrayOf()).toIntOrNull() ?: -1
            Pair(it, version)
        }.maxBy { it.second }?.first
    }

    private fun takeReplica() {
        ensureReplica {
            zk.multi(listOf(
                    Op.create("$SREPLICA_PATH/name", name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL),
                    Op.create("$SREPLICA_PATH/version", version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL),
                    Op.delete("$REPLICAS_PATH/${name()}", -1)
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
                logger.info { "Node ${name()} is now the synchronized replica." }
                runForLeader()
            }
            KeeperException.Code.NODEEXISTS -> {
                runForReplica()
            }
            else -> {
                logger.error { "Error taking role of synchronized replica: ${KeeperException.create(ecode)}" }
            }
        }
    }

    // ----------------
    // Leader election.
    // ----------------

    private fun runForLeader() {
        ensureSynchronized {
            zk.getData(LEADER_WATCH_PATH, false, runForLeaderCb, null)
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
        ensureSynchronized {
            zk.exists(LEADER_WATCH_PATH, watchLeaderWatch, watchLeaderCb, null)
        }
    }

    private val watchLeaderWatch = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert(LEADER_WATCH_PATH.equals(it.path))
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
            KeeperException.Code.NODEEXISTS -> {
                logger.warn { "Leader already elected." }
            }
            else -> {
                logger.error {
                    "Error checking for leader existence: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun takeLeader() {
        ensureSynchronized {
            zk.multi(listOf(
                    Op.delete("$SREPLICA_PATH/name", -1),
                    Op.delete("$SREPLICA_PATH/version", -1),
                    Op.create("$LEADER_PATH/name", name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL),
                    Op.create("$LEADER_PATH/version", version.toString().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
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
                logger.info { "Node ${name()} is now the leader." }
            }
            KeeperException.Code.NODEEXISTS -> {
                runForLeader()
            }
            else -> {
                logger.error { "Error taking role of leader: ${KeeperException.create(ecode)}" }
            }
        }
    }

    // ----------------
    // Replicas.
    // ----------------

    fun countReplicas() {
        zk.getChildren(REPLICAS_PATH, countReplicasWatcher, countReplicasCb, null)
    }

    private val countReplicasWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeChildrenChanged -> {
                assert(REPLICAS_PATH.equals(it.path))
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
     * Makes sure the current node is a synchronized replica
     * before running the supplied callback.
     */
    private fun ensureSynchronized(f: () -> Unit) {
        if (type == NodeType.SYNCHRONIZED_REPLICA) {
            f()
        } else if (type == NodeType.REPLICA) {
            runForReplica()
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

    /**
     * Creates an ephemeral parameter of the replica.
     */
    private fun createParam(path: String, data: ByteArray) {
        zk.create(path, data, OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, createParamCb, Pair(path, data))
    }

    private val createParamCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        val pair = ctx as Pair<String, ByteArray>
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                createParam(pair.first, pair.second)
            }
            KeeperException.Code.OK -> {
                logger.info { "Parameter created $path." }
            }
            KeeperException.Code.NODEEXISTS -> {
                logger.warn { "Parameter already exists $path." }
            }
            else -> {
                logger.error {
                    "Error creating parameter $path: ${KeeperException.create(ecode)}"
                }
            }
        }

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
