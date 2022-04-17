package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.SharedCacheFactory
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.SyncObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.SynchronizedStaticsCache
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncObjectClassResource}

import scala.reflect.ClassTag

class DefaultSynchronizedStaticsCache(channel: CachePacketChannel,
                                      classCenter: SyncClassCenter,
                                      override val defaultContracts: ContractDescriptorData,
                                      override val network: Network) extends DefaultSynchronizedObjectCache[MethodCaller](channel, classCenter, defaultContracts, network) with SynchronizedStaticsCache {

    override protected def getRootContract(factory: SyncObjectContractFactory)(clazz: Class[MethodCaller], context: SyncObjectContext): StructureContract[MethodCaller] = {
        factory.getStaticContract(clazz, context)
    }

}

object DefaultSynchronizedStaticsCache {
    private final val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    def apply[A <: AnyRef : ClassTag](contracts: ContractDescriptorData): SharedCacheFactory[SynchronizedStaticsCache] = {
        channel => {
            apply(channel, contracts, channel.traffic.connection.network)
        }
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](contracts: ContractDescriptorData, network: Network): SharedCacheFactory[SynchronizedStaticsCache] = {
        channel => {
            apply(channel, contracts, network)
        }
    }

    private def apply(channel: CachePacketChannel, contracts: ContractDescriptorData, network: Network): SynchronizedStaticsCache = {
        import fr.linkit.engine.application.resource.external.LocalResourceFolder._
        val context   = channel.manager.network.connection.getApp
        val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val generator = new DefaultSyncClassCenter(context.compilerCenter, resources)

        new DefaultSynchronizedStaticsCache(channel, generator, contracts, network)
    }

}
