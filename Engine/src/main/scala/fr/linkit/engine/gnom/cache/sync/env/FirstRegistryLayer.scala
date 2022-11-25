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

import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.cache.sync.env._
import fr.linkit.api.gnom.cache.sync.instantiation.SyncObjectInstantiator
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.CacheRepoContent
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.FirstFloorObjectProfile
import fr.linkit.engine.gnom.cache.sync.env.node.ConnectedObjectDataSupplier
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{AnyRefPacket, ObjectPacket}
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks
import fr.linkit.engine.internal.debug.{ConnectedObjectTreeRetrievalStep, Debugger, RequestStep}

class FirstRegistryLayer[A <: AnyRef](nph         : CORNPH,
                                      selector    : EngineSelector,
                                      defaultPool : Procrastinator,
                                      channel     : RequestPacketChannel,
                                      instantiator: SyncObjectInstantiator,
                                      dataSupp    : ConnectedObjectDataSupplier,
                                      cacheInfo   : CacheInfo)
                                     (override val secondLayer: SecondRegistryLayer)
        extends ConnectedObjectRegistryLayer[A](nph, selector, defaultPool, channel, instantiator, dataSupp, cacheInfo) {


    override def findCompanion(id: NamedIdentifier): Option[SyncObjectCompanion[A]] = findCompanion(id).orElse {
        requestFirstLayerObject(id)
        findCompanion(id)
    }

    def snapshotContent: CacheRepoContent[A] = {
        def toProfile(node: SyncObjectCompanion[A]): FirstFloorObjectProfile[A] = {
            val syncObject = node.obj
            val mirror     = syncObject.isMirroring || syncObject.isMirrored
            FirstFloorObjectProfile[A](node.id, syncObject, node.ownerTag, mirror)
        }

        val array = companions.values.map(toProfile).toArray
        new CacheRepoContent(array)
    }


    def findCompanionLocal(id: NamedIdentifier): Option[SyncObjectCompanion[A]] = companions.get(id)

    private def requestFirstLayerObject(id: NamedIdentifier): Unit = {
        val ownerTag = cacheInfo.ownerTag
        val ownerNT  = selector.retrieveNT(ownerTag)
        val treeRef  = ConnectedObjectReference(cacheInfo.family, cacheInfo.cacheID, ownerNT, true, id)

        val initLock = NetworkObjectReferencesLocks.getInitializationLock(treeRef)
        val refLock  = NetworkObjectReferencesLocks.getLock(treeRef)
        initLock.lock() //locked only during computation: the lock is released while waiting for the response.
        refLock.lock() //locked during all the request process

        //if the object companion is not present on the owner side, the tree is simply not present so we abort the request
        //if another thread was already performing this request, the current thread had to wait for the
        //above locks to get released, which means that the requested three has been added.
        //We check the second state by checking if the requested tree is finally present on this engine.
        if (!nph.isPresentOnEngine(ownerNT, treeRef) || findCompanionLocal(id).isDefined) {
            refLock.unlock()
            initLock.unlock()
            return
        }
        try {
            //registry.putUnknownTree(id)

            Debugger.push(ConnectedObjectTreeRetrievalStep(treeRef))
            AppLoggers.ConnObj.trace(s"Requesting root object $treeRef.")
            AppLoggers.Debug.info(s"Requesting root object $treeRef.")


            Debugger.push(RequestStep("CO tree retrieval", s"retrieve tree $treeRef", ownerTag, channel.reference))
            val req = channel.makeRequest(ChannelScopes.apply(ownerTag))
                    .addPacket(AnyRefPacket(id))
                    .submit()

            val depth    = initLock.release()
            val response = req.nextResponse
            initLock.depthLock(depth)
            Debugger.pop()
            response.nextPacket[Packet] match {
                case ObjectPacket(profile: FirstFloorObjectProfile[A]) =>
                    register(profile.identifier, profile.owner, new InstanceWrapper[A](profile.obj), profile.mirror)
                case EmptyPacket                                       => //the tree does not exists, do nothing.
            }
            AppLoggers.Debug.info("Ended Tree retrieval execution")
        } finally {
            Debugger.pop()
            initLock.unlock()
            refLock.unlock()
        }
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
