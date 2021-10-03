package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.reference.presence.ObjectNetworkPresence
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

/**
 * The node of a synchronized object.
 * @tparam A the super type of the synchronized object
 */
trait SyncNode[A <: AnyRef] {

    lazy val treePath: Array[Int] = {
        var parent: SyncNode[_] = this
        val buff                = ListBuffer.empty[Int]
        while (parent != null) {
            buff += parent.id
            parent = parent.parent
        }
        buff.toArray.reverse
    }

    val objectPresence: ObjectNetworkPresence

    val location: SyncNodeReference

    /**
     * The tree in which this node is stored.
     */
    val tree: SynchronizedObjectTree[_]

    /**
     * The [[Puppeteer]] of the synchronized object.
     * @see [[Puppeteer]]
     */
    val puppeteer: Puppeteer[A]

    /**
     * The [[Chip]] of the synchronized object.
     * @see [[Chip]]
     */
    val chip: Chip[A]

    /**
     * This node's identifier
     */
    val id: Int

    /**
     * The engine identifier that owns the synchronized object.
     * (The owner is usually the engine that have created the object)
     */
    val ownerID: String

    /**
     * The synchronized object.
     */
    val synchronizedObject: A with SynchronizedObject[A]

    /**
     * This node's parent (null if this node is a root node)
     */
    @Nullable val parent: SyncNode[_]

    /**
     *
     * @param engineID the engine's identifier to test
     * @return true if this node (and thus the synchronized object) is also present on the tested engine.
     */
    def isPresentOnEngine(engineID: String): Boolean

    //TODO Put this for internal use
    def putPresence(engineID: String): Unit

}
