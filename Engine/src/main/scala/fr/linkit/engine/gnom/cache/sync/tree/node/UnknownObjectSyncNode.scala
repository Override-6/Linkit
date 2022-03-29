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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.SyncObjectReference
import fr.linkit.api.gnom.cache.sync.tree.{ObjectSyncNode, SynchronizedObjectTree}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.SynchronizedObjectException

import scala.collection.mutable

class UnknownObjectSyncNode(data: NodeData[AnyRef]) extends MutableSyncNode[AnyRef] {

    override val tree          : SynchronizedObjectTree[_] = data.tree
    override val objectPresence: NetworkObjectPresence     = data.presence
    override val reference     : SyncObjectReference       = data.reference
    override val ownerID       : String                    = data.ownerID
    override val id            : Int                       = reference.nodePath.last
    private  val childs                                    = mutable.HashMap.empty[Int, MutableSyncNode[_]]
    private var parent0        : MutableSyncNode[_]        = data.parent.getOrElse {
        throw new SynchronizedObjectException("Unexpected Unknown Object sync node with no parent")
    }

    override def parent: MutableSyncNode[_] = parent0

    override def discoverParent(node: ObjectSyncNodeImpl[_]): Unit = {
        if (!parent.isInstanceOf[UnknownObjectSyncNode])
            throw new IllegalStateException("Parent already known !")
        parent0 = node
    }

    override def addChild(child: MutableSyncNode[_]): Unit = {
        if (child eq this)
            throw new IllegalArgumentException("can't add self as child")
        childs.put(child.id, child)
    }

    override def getMatchingSyncNode(nonSyncObject: AnyRef): ObjectSyncNode[_ <: AnyRef] = {
        for (child <- childs.values) {
            val found = child.getMatchingSyncNode(nonSyncObject)
            if (found != null)
                return found
        }
        null
    }

    def setAsKnownObjectNode[A <: AnyRef](data: ObjectNodeData[A]): ObjectSyncNode[A] = {
        val parent = data.parent.orNull
        val node   = new ObjectSyncNodeImpl[A](parent, data)
        parent.addChild(node)
        childs.values.foreach(_.discoverParent(node))
        node
    }

}
