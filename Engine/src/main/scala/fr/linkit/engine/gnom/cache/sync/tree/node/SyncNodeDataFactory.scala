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

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

trait SyncNodeDataFactory {

    def newChippedObjectData[A <: AnyRef](parent: MutableSyncNode[_ <: AnyRef], id: Int,
                                          origin: Option[AnyRef],
                                          ownerID: String): ChippedObjectNodeData[A]

    def newSyncObjectData[A <: AnyRef](parent: MutableSyncNode[_ <: AnyRef], id: Int,
                                       syncObject: A with SynchronizedObject[A], origin: Option[AnyRef],
                                       ownerID: String): SyncObjectNodeData[A]

    def newUnknownObjectData[A <: AnyRef](parent: MutableSyncNode[_ <: AnyRef], path: Array[Int]): NodeData[A]
}
