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

package linkit.base.network.statics

import fr.linkit.api.application.resource.local.LocalFolder
import fr.linkit.api.gnom.cache.{SharedCacheFactory, SharedCacheReference}
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.level.ConcreteSyncLevel
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.{StaticsCaller, SynchronizedStaticsCache}
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.generation.{DefaultSyncClassCenter, SyncClassStorageResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic

//this class is used by the statics synchronization side.
//It is not directly used by the user and must uses a specific sync instance creator.
class DefaultConnectedStaticsCache @Persist()(channel              : CachePacketChannel,
                                              classCenter          : SyncClassCenter,
                                              override val contract: ContractDescriptorData,
                                              selector             : EngineSelector,
                                              val defaultPool      : Procrastinator,
                                              cacheManagerLinker   : NetworkPresenceHandler[SharedCacheReference],
                                              omc                  : ObjectManagementChannel)
        extends DefaultConnectedObjectCache[StaticsCaller](channel, classCenter, contract, selector, defaultPool, cacheManagerLinker, omc) with SynchronizedStaticsCache {

    //ensuring that the creator is of the right type
    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: StaticsCaller]): StaticsCaller with SynchronizedObject[StaticsCaller] = {
        checkCreator(creator)
        super.syncObject(id, creator)
    }

    override def getContract[B <: AnyRef](creator: SyncInstanceCreator[B], context: ConnectedObjectContext): StructureContract[B] = {
        creator match {
            case creator: SyncStaticAccessInstanceCreator     =>
                contractFactory.getContract(creator.targetedClass.asInstanceOf[Class[StaticsCaller]], context.withSyncLevel(ConcreteSyncLevel.Statics))
            case InstanceWrapper(methodCaller: StaticsCaller) =>
                val cl = methodCaller.staticsTarget
                contractFactory.getContract[B](cl.asInstanceOf[Class[B]], context.withSyncLevel(ConcreteSyncLevel.Statics))
            case _                                            =>
                throwUOE()
        }
    }

    private def checkCreator(creator: SyncInstanceCreator[_ <: StaticsCaller]): Unit = {
        if (!creator.isInstanceOf[SyncStaticAccessInstanceCreator])
            throwUOE()
    }

    private def throwUOE(): Nothing = {
        throw new UnsupportedOperationException(s"Can only accept Sync Instance Creator of type '${classOf[SyncStaticAccessInstanceCreator].getSimpleName}'.")
    }

}

object DefaultConnectedStaticsCache {

    import SharedCacheFactory.lambdaToFactory

    private final val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir") + "/Classes"

    def apply(contracts: ContractDescriptorData): SharedCacheFactory[SynchronizedStaticsCache] = {
        lambdaToFactory(classOf[DefaultConnectedStaticsCache])(channel => {
            apply(channel, contracts, channel.traffic.connection.network)
        })
    }

    private[linkit] def apply(contracts: ContractDescriptorData, network: Network): SharedCacheFactory[SynchronizedStaticsCache] = {
        lambdaToFactory(classOf[DefaultConnectedStaticsCache])(channel => {
            apply(channel, contracts, network)
        })
    }

    private def apply(channel: CachePacketChannel, contracts: ContractDescriptorData, network: Network): SynchronizedStaticsCache = {
        val app       = channel.manager.network.connection.getApp
        val resources = app.getAppResources.getOrOpen[LocalFolder](ClassesResourceDirectory)
                .getEntry
                .getOrAttachRepresentation[SyncClassStorageResource]("lambdas")
        val generator = new DefaultSyncClassCenter(resources, app.compilerCenter)
        //FIXME ugly
        val omc = network.connection.traffic match {
            case traffic: AbstractPacketTraffic => traffic.getObjectManagementChannel
            case _                              => throw new UnsupportedOperationException("Cannot retrieve object management channel.")
        }
        val cml       = channel.manager.getCachesLinker
        val defaultPool: Procrastinator = network.connection
        new DefaultConnectedStaticsCache(channel, generator, contracts, network, defaultPool, cml, omc)
    }

}
