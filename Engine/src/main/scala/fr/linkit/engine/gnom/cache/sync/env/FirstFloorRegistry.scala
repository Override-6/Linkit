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

package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.ConnectedObject
import fr.linkit.api.gnom.cache.sync.env._
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.CacheRepoContent
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.FirstFloorObjectProfile
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes

class FirstFloorRegistry[A <: AnyRef](registry: ConnectedObjectRegistryImpl[A]) extends ConnectedObjectRegistryLayer[A](registry) {

    private val cache   = registry.cache
    private val channel = registry.cache.channel

    def snapshotContent: CacheRepoContent[A] = {
        def toProfile(node: SyncObjectCompanion[A]): FirstFloorObjectProfile[A] = {
            val syncObject = node.obj
            val mirror     = syncObject.isMirroring || syncObject.isMirrored
            FirstFloorObjectProfile[A](node.id, syncObject, node.ownerTag, mirror)
        }

        val array = companions.values.map(toProfile).toArray
        new CacheRepoContent(array)
    }

    def findCompLocal(id: NamedIdentifier): Option[SyncObjectCompanion[A]] = companions.get(id)

    def findNode(id: NamedIdentifier): Option[SyncObjectCompanion[A]] = findCompLocal(id).orElse {
        cache.requestFirstFloorNode(id)
        findCompLocal(id)
    }

    override protected def onCompanionRegistered(comp: SyncObjectCompanion[A]): Unit = {
        val ownerTag    = comp.ownerTag
        val treeProfile = FirstFloorObjectProfile(comp.id, comp.obj, ownerTag, comp.isMirror)
        AppLoggers.ConnObj.debug(s"Notifying owner that a new connected object has been added on the cache.")
        channel.makeRequest(ChannelScopes.apply(ownerTag))
                .addPacket(ObjectPacket(treeProfile))
                .submit()
    }


    override protected def initObject(obj: ConnectedObject[A]): Unit = {

    }
}
