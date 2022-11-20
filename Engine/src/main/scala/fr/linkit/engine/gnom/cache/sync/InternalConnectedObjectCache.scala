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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncObjectInstantiator}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.engine.gnom.cache.sync.env.node.NodeDataFactory

trait InternalConnectedObjectCache[A <: AnyRef] extends ConnectedObjectCache[A] with NodeDataFactory {
    def requestFirstFloorNode(id: NamedIdentifier): Unit

    def getContract(creator: SyncInstanceCreator[A],
                    context: ConnectedObjectContext): StructureContract[A]

    val channel            : RequestPacketChannel
    val defaultInstantiator: SyncObjectInstantiator
}
