package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.cache.sync.env.{ConnectedObjectRegistry, SyncObjectCompanion}
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.referencing.NetworkObject
import fr.linkit.api.gnom.referencing.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.engine.gnom.cache.sync.InternalConnectedObjectCache
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler

class ConnectedObjectRegistryImpl[A <: AnyRef](val cache   : InternalConnectedObjectCache[A],
                                               nphParent   : NetworkPresenceHandler[SharedCacheReference],
                                               omc         : ObjectManagementChannel,
                                               val selector: EngineSelector) extends
        AbstractNetworkPresenceHandler[ConnectedObjectReference](Some(nphParent), omc, selector)
        with InitialisableNetworkObjectLinker[ConnectedObjectReference] with ConnectedObjectRegistry[A] {

    private[env] val firstFloor  = new FirstFloorRegistry[A](this)
    private[env] val secondFloor = new SecondRegistryLayer(this)


    override def initializeObject(obj: NetworkObject[_ <: ConnectedObjectReference]): Unit = {

    }

    override def registerReference(ref: ConnectedObjectReference): Unit = {
        super.registerReference(ref)
    }

    override def unregisterReference(ref: ConnectedObjectReference): Unit = {
        super.unregisterReference(ref)
    }


    override def findObject(reference: ConnectedObjectReference): Option[NetworkObject[ConnectedObjectReference]] = {
        if (reference.cacheID != cache.cacheID || reference.family != cache.family)
            return None
        companions.get(reference.identifier).map(_.obj)
    }


    private[env] def findNodeFromOrigin(origin: A): Option[SyncObjectCompanion[A]] = origins.get(origin)

    /*/*
    * used to store objects whose synchronized version of keys already have bound references.
    * */
    private   val linkedOrigins = mutable.HashMap.empty[A, ConnectedObjectReference]


    def linkWithReference(obj: A, ref: ConnectedObjectReference): Unit = {
        linkedOrigins(obj) = ref
    }

    def removeLinkedReference(obj: A): Option[ConnectedObjectReference] = linkedOrigins.remove(obj)
*/


}