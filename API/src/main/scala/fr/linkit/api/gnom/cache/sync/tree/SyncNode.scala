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

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.SynchronizedStructureBehavior
import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import org.jetbrains.annotations.Nullable

import scala.collection.mutable.ListBuffer

/**
 * The node of a synchronized object.
 * @tparam A the super type of the synchronized object
 */
trait SyncNode[A <: AnyRef] {

    val objectPresence: NetworkObjectPresence

    val reference: SyncObjectReference

    val behavior: SynchronizedStructureBehavior[A]

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

    lazy val treePath: Array[Int] = {
        var parent: SyncNode[_] = this
        val buff                = ListBuffer.empty[Int]
        while (parent != null) {
            buff += parent.id
            parent = parent.parent
        }
        buff.toArray.reverse
    }
}
