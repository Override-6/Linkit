package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.{SynchronizedObject, SynchronizedObjectCenter}
import fr.linkit.api.connection.cache.obj.IllegalSynchronizationException


trait SynchronizedObjectTree[A <: AnyRef] {

    /**
     * The center of the tree
     */
    val center: SynchronizedObjectCenter[A]

    /**
     * This tree's identifier (rootNode.id == this.id)
     */
    val id: Int

    /**
     * The behavior store of this object's tree
     */
    val behaviorStore: SynchronizedObjectBehaviorStore

    /**
     *
     * @return the root node of this object.
     */
    def rootNode: SyncNode[A]

    /**
     * Retrieves a node from it's path (see [[fr.linkit.api.connection.cache.obj.description.SyncNode.treePath]]
     *
     * @param path the path of the node.
     * @tparam B the type of the node's synchronized object
     * @return Some(SyncNode) if found, None instead.
     */
    def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]]

    /**
     *
     * Inserts an object in the tree, transforms it into a synchronized object, and wraps the resulting object in a [[SyncNode]]
     *
     * @throws IllegalSynchronizationException if the object is already synchronized.
     * @throws IllegalArgumentException        if the given parent does not belongs to this tree.
     * @throws NoSuchSyncNodeException         if the parent's path could not be found.
     * @param parent  the parent of the synchronized object that will be inserted.
     * @param id      the identifier of the created node
     * @param obj     the object that will be converted to a synchronized object. (The given object may be corrupted)
     * @param ownerID the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    //TODO Should be removed (or only used internally)
    def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, obj: B, ownerID: String): SyncNode[B]

    /**
     *
     * Inserts an object in the tree, transforms it into a synchronized object, and wraps the resulting object in a [[SyncNode]]
     *
     * @throws IllegalSynchronizationException if the object is already synchronized.
     * @throws IllegalArgumentException        if the given parent is does not belongs to this tree.
     * @throws NoSuchSyncNodeException         if the parent's path could not be found.
     * @param parentPath the parent's path of the synchronized object that will be inserted.
     * @param id         the identifier of the created node
     * @param obj        the object that will be converted to a synchronized object. (The given object may be corrupted)
     * @param ownerID    the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B]

}
