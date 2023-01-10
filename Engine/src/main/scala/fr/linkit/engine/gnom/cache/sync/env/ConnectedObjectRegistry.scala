package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.env.{ObjectConnector, SyncObjectCompanion}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncObjectInstantiator
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NetworkObject
import fr.linkit.api.gnom.referencing.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.env.node.ConnectedObjectDataSupplier
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler

class ConnectedObjectRegistry[A <: AnyRef](nphParent   : NetworkPresenceHandler[SharedCacheReference],
                                           omc         : ObjectManagementChannel,
                                           defaultPool : Procrastinator,
                                           channel     : RequestPacketChannel,
                                           instantiator: SyncObjectInstantiator,
                                           dataSupp    : ConnectedObjectDataSupplier,
                                           cacheInfo   : CacheInfo,
                                           selector    : EngineSelector) extends
        AbstractNetworkPresenceHandler[ConnectedObjectReference](Some(nphParent), omc, selector)
        with InitialisableNetworkObjectLinker[ConnectedObjectReference] with CORNPH {

    private[sync] val secondLayer = new SecondRegistryLayer(this, selector, defaultPool, channel, instantiator, dataSupp, cacheInfo)
    private[sync] val firstLayer  = new FirstRegistryLayer[A](this, selector, defaultPool, channel, instantiator, dataSupp, cacheInfo)(secondLayer)

    val connector: ObjectConnector = secondLayer

    override def initializeObject(obj: NetworkObject[_ <: ConnectedObjectReference]): Unit = {
        obj match {
            case co: A with SynchronizedObject[A] if obj.reference.isFirstLayer => firstLayer.initObject(co)
            case co: SynchronizedObject[AnyRef]                                 => secondLayer.initObject(co)
        }

    }


    override def registerReference(ref: ConnectedObjectReference): Unit = super.registerReference(ref)

    override def unregisterReference(ref: ConnectedObjectReference): Unit = super.unregisterReference(ref)

    def findCompanion(reference: ConnectedObjectReference): Option[SyncObjectCompanion[AnyRef]] = {
        if (reference.cacheID != cacheInfo.cacheID || reference.family != cacheInfo.family)
            return None
        val id = reference.identifier
        if (reference.isFirstLayer)
            firstLayer.findCompanionLocal(id).map(_.asInstanceOf[SyncObjectCompanion[AnyRef]])
        else
            secondLayer.findCompanion(id)
    }

    override def findObject(reference: ConnectedObjectReference): Option[NetworkObject[ConnectedObjectReference]] = {
        findCompanion(reference).map(_.obj)
    }

}