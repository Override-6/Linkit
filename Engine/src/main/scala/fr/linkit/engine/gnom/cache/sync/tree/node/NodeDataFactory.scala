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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.{ChippedObject, SynchronizedObject}
import fr.linkit.api.gnom.network.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier

trait NodeDataFactory {

    def newNodeData[A <: AnyRef, N <: NodeData[A]](req: NodeDataRequest[A, N]): N
}

sealed trait NodeDataRequest[A <: AnyRef, +N <: NodeData[A]] {
    val parent: MutableNode[_ <: AnyRef]
}

class NormalNodeDataRequest[A <: AnyRef](val parent : MutableNode[_ <: AnyRef],
                                         val path   : Array[NamedIdentifier],
                                         val ownerID: UniqueTag with NetworkFriendlyEngineTag) extends NodeDataRequest[A, NodeData[A]]

class SyncNodeDataRequest[A <: AnyRef](parent        : MutableNode[_ <: AnyRef],
                                       id            : NamedIdentifier,
                                       val syncObject: A with SynchronizedObject[A],
                                       val origin    : Option[A],
                                       ownerID       : UniqueTag with NetworkFriendlyEngineTag,
                                       val syncLevel : SyncLevel)
        extends ChippedObjectNodeDataRequest[A](parent, id, syncObject, ownerID) with NodeDataRequest[A, SyncObjectNodeData[A]]

class ChippedObjectNodeDataRequest[A <: AnyRef](val parent       : MutableNode[_ <: AnyRef],
                                                val id           : NamedIdentifier,
                                                val chippedObject: ChippedObject[A],
                                                val ownerID      : UniqueTag with NetworkFriendlyEngineTag)
        extends NodeDataRequest[A, ChippedObjectNodeData[A]]
