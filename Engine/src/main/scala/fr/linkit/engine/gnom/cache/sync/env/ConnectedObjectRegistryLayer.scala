package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.cache.sync.contract.level.ConcreteSyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.level.MirrorableSyncLevel
import fr.linkit.api.gnom.cache.sync.env.SyncObjectCompanion
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncObjectInstantiator}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.{ConnectedObjectAlreadyRegisteredException, ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.network.tag.{EngineSelector, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.env.node._
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.cache.sync.invokation.UsageConnectedObjectContext
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.ObjectPuppeteer
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks

import scala.collection.mutable

abstract class ConnectedObjectRegistryLayer[A <: AnyRef](nph         : CORNPH,
                                                         selector    : EngineSelector,
                                                         defaultPool : Procrastinator,
                                                         channel     : RequestPacketChannel,
                                                         instantiator: SyncObjectInstantiator,
                                                         dataSupp    : ConnectedObjectDataSupplier,
                                                         cacheInfo   : CacheInfo) {

    protected val companions        = mutable.HashMap.empty[NamedIdentifier, SyncObjectCompanion[A]]
    protected val companionsOrigins = mutable.WeakHashMap.empty[A, SyncObjectCompanion[A]]


    def secondLayer: SecondRegistryLayer

    protected def findCompanionFromOrigin(origin: A): Option[SyncObjectCompanion[A]] = companionsOrigins.get(origin)


    def findCompanion(id: NamedIdentifier): Option[SyncObjectCompanion[A]]

    private def registerComp(comp: SyncObjectCompanion[A], originOpt: Option[A]): Unit = {
        companions.put(comp.id, comp)
        for (origin <- originOpt)
            companionsOrigins.put(origin, comp)
        onCompanionRegistered(comp)
    }

    def initObject(obj: A with SynchronizedObject[A]): Unit = {
        val reference = obj.reference
        register(reference.identifier, reference.owner, InstanceWrapper[A](obj), obj.isMirrored)
    }

    protected def onCompanionRegistered(comp: SyncObjectCompanion[A]): Unit = ()


    def register(id             : NamedIdentifier,
                 rootObjectOwner: UniqueTag with NetworkFriendlyEngineTag,
                 creator        : SyncInstanceCreator[A],
                 mirror         : Boolean): SyncObjectCompanion[A] = {


        creator match {
            case InstanceWrapper(obj) =>
                companions.get(id) match {
                    case Some(companion) =>
                        if (companion.obj eq obj)
                            companion
                        else
                            throw new ConnectedObjectAlreadyRegisteredException(s"object '$id' already registered.")
                    case None            => registerNew(id, rootObjectOwner, creator, mirror)
                }
            case _ => registerNew(id, rootObjectOwner, creator, mirror)
        }
    }

    private def registerNew(id             : NamedIdentifier,
                            rootObjectOwner: UniqueTag with NetworkFriendlyEngineTag,
                            creator        : SyncInstanceCreator[A],
                            mirror         : Boolean): SyncObjectCompanion[A] = {
        val rootObjectOwnerNT = selector.retrieveNT(rootObjectOwner)
        val nodeReference     = ConnectedObjectReference(cacheInfo.family, cacheInfo.cacheID, rootObjectOwnerNT, true, id)

        val lock = NetworkObjectReferencesLocks.getInitializationLock(nodeReference)
        lock.lock()
        try {
            val choreographer = new InvocationChoreographer()
            val syncLevel     = if (mirror) MirrorableSyncLevel.Mirror else Synchronized //root objects can either be mirrored or fully sync objects.
            val context       = UsageConnectedObjectContext(
                rootObjectOwnerNT,
                creator.syncClassDef, syncLevel,
                choreographer, selector)

            val contract = dataSupp.getContract(creator, context)
            val obj      = instantiator.newSynchronizedInstance[A](creator)


            val chip      = ObjectChip[A](contract, selector, defaultPool, obj)
            val puppeteer = ObjectPuppeteer[A](channel, nodeReference)
            val presence  = nph.getPresence(nodeReference)

            val baseData = new CompanionData[A](nodeReference, presence, rootObjectOwner, selector)
            val chipData = new ChippedObjectCompanionData[A](selector, chip, contract, choreographer, secondLayer, obj)(baseData)
            val syncData = new SyncObjectCompanionData[A](puppeteer, obj, syncLevel)(chipData)
            val node     = new SyncObjectCompanionImpl[A](syncData)
            registerComp(node: SyncObjectCompanion[A], creator.getOrigin)
            node
        } finally lock.unlock()
    }

}
