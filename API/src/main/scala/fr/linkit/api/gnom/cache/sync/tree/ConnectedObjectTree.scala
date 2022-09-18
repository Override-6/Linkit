/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectContractFactory
import fr.linkit.api.gnom.referencing.NamedIdentifier


trait ConnectedObjectTree[A <: AnyRef] {

    /**
     * This tree's identifier (rootNode.id == this.id)
     */
    val id: NamedIdentifier

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
    def findNode[B <: AnyRef](path: Array[NamedIdentifier]): Option[ConnectedObjectNode[B]]

}
