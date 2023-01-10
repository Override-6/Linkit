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

package fr.linkit.engine.gnom.cache.sync.env.node

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.level.SyncLevel
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.sync.{ChippedObject, SynchronizedObject}
import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.referencing.NamedIdentifier

trait ConnectedObjectDataSupplier {
    def getContract[A <: AnyRef](creator: SyncInstanceCreator[A],
                                 context: ConnectedObjectContext): StructureContract[A]

    def newNodeData[A <: AnyRef, N <: CompanionData[A]](req: NodeDataRequest[A, N]): N
}

sealed trait NodeDataRequest[A <: AnyRef, +N <: CompanionData[A]]

class NormalNodeDataRequest[A <: AnyRef](val identifier: NamedIdentifier,
                                         val firstFloor: Boolean,
                                         val ownerTag  : NameTag) extends NodeDataRequest[A, CompanionData[A]]

class SyncNodeDataRequest[A <: AnyRef](id                          : NamedIdentifier,
                                       ownerID                     : NameTag,
                                       firstFloor                  : Boolean,
                                       val syncObject: A with SynchronizedObject[A],
                                       val syncLevel               : SyncLevel)
        extends ChippedObjectNodeDataRequest[A](id, ownerID, firstFloor, syncObject) with NodeDataRequest[A, SyncObjectCompanionData[A]]

class ChippedObjectNodeDataRequest[A <: AnyRef](val id                 : NamedIdentifier,
                                                val ownerTag           : NameTag,
                                                val firstFloor         : Boolean,
                                                val connectedObject: ChippedObject[A])
        extends NodeDataRequest[A, ChippedObjectCompanionData[A]]
