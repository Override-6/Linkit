/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.{SynchronizedObject, SynchronizedObjectCache}
import fr.linkit.api.gnom.cache.sync.CanNotSynchronizeException
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectContractFactory


trait SynchronizedObjectTree[A <: AnyRef] {

    /**
     * This tree's identifier (rootNode.id == this.id)
     */
    val id: Int

    /**
     * The behavior store of this object's tree
     */
    val contractFactory: ObjectContractFactory

    /**
     *
     * @return the root node of this object.
     */
    def rootNode: ObjectSyncNode[A]

    /**
     * Retrieves a node from it's path (see [[fr.linkit.api.gnom.cache.sync.contract.description.SyncNode.treePath]]
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
     * @throws CanNotSynchronizeException if the object is already synchronized.
     * @throws IllegalArgumentException   if the given parent does not belongs to this tree.
     * @throws NoSuchSyncNodeException    if the parent's path could not be found.
     * @param parent  the parent of the synchronized object that will be inserted.
     * @param id      the identifier of the created node
     * @param source     the object that will be converted to a synchronized object. (The given object may be corrupted)
     * @param ownerID the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    //TODO Should be removed (or only used internally)
    def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, source: B, ownerID: String): ObjectSyncNode[B]

    /**
     *
     * Inserts an object in the tree, transforms it into a synchronized object, and wraps the resulting object in a [[SyncNode]]
     *
     * @throws CanNotSynchronizeException if the object is already synchronized.
     * @throws IllegalArgumentException   if the given parent is does not belongs to this tree.
     * @throws NoSuchSyncNodeException    if the parent's path could not be found.
     * @param parentPath the parent's path of the synchronized object that will be inserted.
     * @param id         the identifier of the created node
     * @param source        the object that will be converted to a synchronized object. (The given object may be corrupted)
     * @param ownerID    the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, source: B, ownerID: String): ObjectSyncNode[B]

}
