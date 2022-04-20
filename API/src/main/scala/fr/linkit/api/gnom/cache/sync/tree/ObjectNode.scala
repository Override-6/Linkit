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

import fr.linkit.api.gnom.cache.sync.SyncObjectReference
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

/**
 * The node of a synchronized object.
 *
 * @tparam A the super type of the synchronized object
 */
trait ObjectNode[A <: AnyRef] {

    val tree: SynchronizedObjectTree[_]

    val objectPresence: NetworkObjectPresence

    val reference: SyncObjectReference

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
     * This node's parent (null if this node is a root node)
     */
    @Nullable def parent: ObjectNode[_]

    lazy val treePath: Array[Int] = {
        var parent: ObjectNode[_] = this
        val buff                = ListBuffer.empty[Int]
        while (parent != null) {
            buff += parent.id
            parent = parent.parent
        }
        buff.toArray.reverse
    }
}
