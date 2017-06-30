package org.botellier.node

import mu.KLogging
import org.apache.zookeeper.*
import org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE
import org.apache.zookeeper.data.Stat
import java.util.*
import kotlin.concurrent.thread

class Node (zooServers: String) : Watcher {
    companion object : KLogging() // sets up logging.

    val id = Integer.toHexString(Random().nextInt())
    val zk = ZooKeeper(zooServers, 4000, this)
    var type = NodeType.REPLICA
        private set

    private var version = 0
    private var totalReplicas = 1 // counting 'this'

    // Candidates cache.


    // ----------------
    // Bootstrap.
    // ----------------

    /**
     * Creates all the required persistent directories if they
     * don't exist.
     */
    fun bootstrap() {
        createDirectory("/replicas")
        createDirectory("/changes")
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
        zk.create("/replicas/${name()}", null, OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, registerCb, null)
    }

    private val registerCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                register()
            }
            KeeperException.Code.OK -> {
                logger.info { "Node successfully registered: $path" }
                updateVersion()
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
        zk.setData("/replicas/${name()}", version.toString().toByteArray(), version, updateVersionCb, null)
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
    // Leader election.
    // ----------------
    // Nodes usually try to run for synchronized replica first. After that, they try to take the leader
    // role if the spot is available.

    fun runForReplica() {
        zk.getData("/sreplica", false, runForReplicaCb, null)
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
                } else {
                    watchReplica()
                    type = NodeType.REPLICA
                }
            }
            KeeperException.Code.NONODE -> {
                createCandidature()
            }
            else -> {
                logger.error {
                    "Error running for synchronized replica: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun watchReplica() {
        zk.exists("/sreplica", watchReplicaWatcher, watchReplicaCb, null)
    }

    private val watchReplicaWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert("/sreplica".equals(it.path))
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

    private fun createCandidature() {
        zk.create(
                "/candidates/${name()}",
                version.toString().toByteArray(),
                OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                createCandidatureCb,
                null
        )
    }

    private val createCandidatureCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                createCandidature()
            }
            KeeperException.Code.OK -> {
                watchCandidature()
            }
            KeeperException.Code.NODEEXISTS -> {
                logger.warn { "Node already a candidate: $path" }
            }
            else -> {
                logger.error {
                    "Error creating candidate $path: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun watchCandidature() {
        zk.getChildren("/candidates", watchCandidatureWatcher, watchCandidatureCb, null)
    }

    private val watchCandidatureWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeChildrenChanged -> {
                assert("/candidates".equals(it.path))
                watchCandidature()
            }
        }
    }

    private val watchCandidatureCb = AsyncCallback.ChildrenCallback { rc, path, ctx, children ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                watchCandidature()
            }
            KeeperException.Code.OK -> {
                if (children == null || children.size < totalReplicas) {
                    watchCandidature()
                } else {
                    thread { evalCandidates(children) }
                }
            }
            else -> {
                logger.error {
                    "Error getting candidates: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun evalCandidates(candidates: List<String>) {
        val bestCandidate = candidates.map {
            val version = String(getCandidateData(it) ?: byteArrayOf()).toIntOrNull() ?: -1
            Pair(it, version)
        }.maxBy { it.second }

        if (bestCandidate != null) {
           if (bestCandidate.first == name()) {
               takeReplica()
           }
        } else {
            logger.warn { "No best candidate for synchronized replica found." }
        }
    }

    private fun getCandidateData(candidate: String): ByteArray? {
        while (true) {
            try {
                val stat = Stat()
                val data = zk.getData("/candidates/$candidate", false, stat)
                return data
            } catch (e: KeeperException.NoNodeException) {
                logger.error { "Trying to get data of invalid candidate $candidate." }
                break
            } catch (e: KeeperException.ConnectionLossException) {
                // Ignore exception and try again.
            }
        }
        return null
    }

    private fun takeReplica() {
        zk.create("/sreplica", name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, takeReplicaCb, null)
    }

    private val takeReplicaCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                takeReplica()
            }
            KeeperException.Code.OK -> {
                type = NodeType.SYNCHRONIZED_REPLICA
                logger.info { "Node ${name()} is now the synchronized replica." }
            }
            KeeperException.Code.NODEEXISTS -> {
                runForReplica()
            }
            else -> {
                logger.error {
                    "Error take role of synchronized replica: ${KeeperException.create(ecode)}"
                }
            }
        }
    }

    private fun runForLeader() {
        zk.exists("/leader", leaderWatch, leaderCb, null)
    }

    private val leaderWatch = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeDeleted -> {
                assert("/leader".equals(it.path))
                runForLeader()
            }
        }
    }

    private val leaderCb = AsyncCallback.StatCallback { rc, path, ctx, stat ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                runForLeader()
            }
            KeeperException.Code.OK -> {
                if (stat == null) {
                    takeLeader()
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
        zk.create("/leader", name().toByteArray(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, takeLeaderCb, null)
    }

    private val takeLeaderCb = AsyncCallback.StringCallback { rc, path, ctx, name ->
        val ecode = KeeperException.Code.get(rc)
        when (ecode) {
            KeeperException.Code.CONNECTIONLOSS -> {
                runForLeader()
            }
            KeeperException.Code.OK -> {
                type = NodeType.LEADER
                logger.info { "Node ${name()} is now the leader."}
            }
        }
    }

    // ----------------
    // Replicas.
    // ----------------

    fun countReplicas() {
        zk.getChildren("/replicas", countReplicasWatcher, countReplicasCb, null)
    }

    private val countReplicasWatcher = Watcher {
        when (it.type) {
            Watcher.Event.EventType.NodeChildrenChanged -> {
                assert("/replicas".equals(it.path))
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
