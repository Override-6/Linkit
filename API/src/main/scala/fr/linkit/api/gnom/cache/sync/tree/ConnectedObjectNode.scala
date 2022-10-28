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

import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

/**
 * The node of a synchronized object.
 *
 * @tparam A the super type of the synchronized object
 */
trait ConnectedObjectNode[A <: AnyRef] {

    val tree: ConnectedObjectTree[_]

    val objectPresence: NetworkObjectPresence

    val reference: ConnectedObjectReference

    /**
     * This node's identifier
     */
    val id: NamedIdentifier

    /**
     * The engine identifier that owns the synchronized object.
     * (The owner is usually the engine that have created the object)
     */
    val ownerTag: UniqueTag with NetworkFriendlyEngineTag

    /**
     * This node's parent (null if this node is a root node)
     */
    @Nullable def parent: ConnectedObjectNode[_]

    def obj: ConnectedObject[A]

    lazy val nodePath: Array[NamedIdentifier] = {
        var parent: ConnectedObjectNode[_] = this
        val buff                = ListBuffer.empty[NamedIdentifier]
        while (parent != null) {
            buff += parent.id
            parent = parent.parent
        }
        buff.toArray.reverse
    }
}
