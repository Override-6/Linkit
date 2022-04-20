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

import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.engine.gnom.cache.sync.{AbstractSynchronizedObject, IllegalSynchronizedObjectException}

class ObjectSyncNodeImpl[A <: AnyRef](private var parent0: ObjectNode[_],
                                      data: SyncObjectNodeData[A]) extends ChippedObjectNodeImpl[A](parent0, data) with InternalObjectSyncNode[A] {

    override val puppeteer         : Puppeteer[A]                 = data.puppeteer
    override val synchronizedObject: A with SynchronizedObject[A] = data.synchronizedObject

    initSyncObject()

    override def parent: ObjectNode[_] = parent0

    override def discoverParent(node: ObjectSyncNodeImpl[_]): Unit = {
        if (!parent.isInstanceOf[UnknownObjectSyncNode])
            throw new IllegalStateException("Parent already known !")

        this.parent0 = parent0
    }

    override def addChild(node: MutableSyncNode[_]): Unit = {
        if (node.parent ne this)
            throw new CanNotSynchronizeException("Attempted to add a child to this node that does not define this node as its parent.")
        if (node eq this)
            throw new IllegalArgumentException("can't add self as child")

        def put(): Unit = childs.put(node.id, node)

        childs.get(node.id) match {
            case Some(value) => value match {
                case _: UnknownObjectSyncNode => put()
                case _: ObjectSyncNodeImpl[_] =>
                    throw new IllegalStateException(s"A Synchronized Object Node already exists at ${puppeteer.nodeLocation.nodePath.mkString("/") + s"/$id"}")
            }
            case None        => put()
        }
    }

    override def toString: String = s"node $reference for sync object ${synchronizedObject.getClass.getName}"


    private def initSyncObject(): Unit = {
        synchronizedObject match {
            case sync: AbstractSynchronizedObject[A] => sync.initialize(this)
            case _                                   => throw new IllegalSynchronizedObjectException(
                "Received unknown kind of synchronized object: " +
                    "could not initialize synchronized object because received object does not extends AbstractSynchronizedObject.")
        }
    }

}
