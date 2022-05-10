package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.SharedCacheFactory
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.{StaticsCaller, SynchronizedStaticsCache}
import fr.linkit.api.gnom.persistence.context.Persist
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper

import scala.reflect.ClassTag

//this class is used by the statics synchronization side.
//It is not directly used by the user and must uses a specific sync instance creator.
class DefaultSynchronizedStaticsCache @Persist()(channel: CachePacketChannel,
                                      classCenter: SyncClassCenter,
                                      override val defaultContracts: ContractDescriptorData,
                                      override val network: Network) extends DefaultSynchronizedObjectCache[StaticsCaller](channel, classCenter, defaultContracts, network) with SynchronizedStaticsCache {

    //ensuring that the creator is of the right type
    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: StaticsCaller]): StaticsCaller with SynchronizedObject[StaticsCaller] = {
        checkCreator(creator)
        super.syncObject(id, creator)
    }

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: StaticsCaller], contracts: ContractDescriptorData): StaticsCaller with SynchronizedObject[StaticsCaller] = {
        checkCreator(creator)
        super.syncObject(id, creator, contracts)
    }

    override protected def getRootContract(factory: SyncObjectContractFactory)(creator: SyncInstanceCreator[StaticsCaller], context: ConnectedObjectContext): StructureContract[StaticsCaller] = {
        creator match {
            case creator: SyncStaticAccessInstanceCreator =>
                factory.getStaticContract(creator.targettedClass.asInstanceOf[Class[StaticsCaller]], context)
            case InstanceWrapper(methodCaller: StaticsCaller) =>
                factory.getStaticContract(methodCaller.staticsTarget.asInstanceOf[Class[StaticsCaller]], context)
            case _                                        => throwUOE()
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
