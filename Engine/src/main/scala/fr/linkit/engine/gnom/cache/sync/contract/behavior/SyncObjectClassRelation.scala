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

package fr.linkit.engine.gnom.cache.sync.contract.behavior

import fr.linkit.engine.gnom.cache.sync.contract.behavior.builder.ObjectBehaviorDescriptor

import scala.collection.mutable.ListBuffer

class SyncObjectClassRelation[A <: AnyRef](descriptor: ObjectBehaviorDescriptor[A], nextSuperRelation: SyncObjectClassRelation[_ >: A]) {

    val targetClass: Class[A] = descriptor.targetClass
    private val interfaceRelation = ListBuffer.empty[SyncObjectClassRelation[_ >: A]]

    def addInterface(interface: SyncObjectClassRelation[_ >: A]): Unit = {
        if (interface.targetClass != classOf[Object] && !interfaceRelation.contains(interface))
            interfaceRelation += interface
    }

    def toNode: BehaviorDescriptorNode[A] = {
        val nextSuperNode = if (nextSuperRelation == null) null else nextSuperRelation.toNode
        new BehaviorDescriptorNode[A](descriptor, nextSuperNode, interfaceRelation.map(_.toNode).toArray)
    }

}
