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

package fr.linkit.engine.gnom.cache.sync.behavior.v2

import fr.linkit.api.gnom.cache.sync.behavior.build.ObjectBehaviorDescriptor

import scala.collection.mutable.ListBuffer

class SyncObjectClassRelation[A <: AnyRef](descriptor: ObjectBehaviorDescriptor[A]) {

    private val relations = ListBuffer.empty[SyncObjectClassRelation[_ >: A]]

    def addRelation[S >: A](relation: SyncObjectClassRelation[S]): Unit = {
        relations += relation
    }

    def countRelations: Int = relations.size

    def toNode: BehaviorDescriptorNode[A] = {
        new BehaviorDescriptorNode[A](descriptor, relations.map(_.toNode).toArray)
    }

}
