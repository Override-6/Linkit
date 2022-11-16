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

import fr.linkit.api.gnom.cache.sync.tree.{ConnectedObjectTree, ObjectSyncNode}
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.SynchronizedObjectException

import scala.collection.mutable

class UnknownObjectSyncNode(data: NodeData[AnyRef]) extends MutableSyncNode[AnyRef] {

    override val tree          : ConnectedObjectTree[_]                  = data.tree
    override val objectPresence: NetworkObjectPresence                   = data.presence
    override val reference     : ConnectedObjectReference                = data.reference
    override val ownerTag      : UniqueTag with NetworkFriendlyEngineTag = data.ownerID
    override val id            : NamedIdentifier                         = reference.nodePath.last
    private  val childs                                                  = mutable.HashMap.empty[NamedIdentifier, MutableNode[_]]

    private var parent0: MutableNode[_] = data.parent.getOrElse {
        throw new SynchronizedObjectException("Unexpected Unknown Object sync node with no parent")
    }

    override def obj: ConnectedObject[AnyRef] = throw new NoSuchElementException(s"Unknown connected Object referenced at '$reference'")

    override def parent: MutableNode[_] = parent0

    override def discoverParent(node: ObjectSyncNodeImpl[_]): Unit = {
        if (!parent.isInstanceOf[UnknownObjectSyncNode])
            throw new IllegalStateException("Parent already known !")
        parent0 = node
    }

    override def addChild(child: MutableNode[_]): Unit = {
        if (child eq this)
            throw new IllegalArgumentException("can't add self as child")
        childs.put(child.id, child)
    }

    override def getChild[B <: AnyRef](id: NamedIdentifier): Option[MutableNode[B]] = childs.get(id).asInstanceOf[Option[MutableNode[B]]]

    override def getMatchingSyncNode(origin: AnyRef): MutableSyncNode[_ <: AnyRef] = {
        for (child <- childs.values) child match {
            case child: MutableSyncNode[_] =>
                val found = child.getMatchingSyncNode(origin)
                if (found != null)
                    return found
        }
        null
    }

    def setAsKnownObjectNode[A <: AnyRef](data: SyncObjectNodeData[A]): ObjectSyncNode[A] = {
        val parent = data.parent.orNull
        val node   = new ObjectSyncNodeImpl[A](data)
        parent.addChild(node)
        childs.values.foreach(_.discoverParent(node))
        node
    }

}
