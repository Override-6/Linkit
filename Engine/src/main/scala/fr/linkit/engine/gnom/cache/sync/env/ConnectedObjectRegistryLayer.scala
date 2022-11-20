package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.env.SyncObjectCompanion
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectAlreadyRegisteredException, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag.{EngineSelector, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.env.node.{ChippedObjectCompanionData, CompanionData, SyncObjectCompanionData, SyncObjectCompanionImpl}
import fr.linkit.engine.gnom.cache.sync.invokation.UsageConnectedObjectContext
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.ObjectPuppeteer
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks

import java.lang.ref.WeakReference
import scala.collection.mutable

abstract class ConnectedObjectRegistryLayer[A <: AnyRef](selector: EngineSelector,
                                                         defaultPool: Procrastinator) {

    protected val companions = mutable.HashMap.empty[NamedIdentifier, SyncObjectCompanion[A]]
    protected val origins    = mutable.HashMap.empty[A, SyncObjectCompanion[A]]

    private val cache               = registry.cache

    private def registerComp(comp: SyncObjectCompanion[A]): Unit = {
        companions.put(comp.id, comp)
        onCompanionRegistered(comp)
    }

    def initObject(obj: ConnectedObject[A]): Unit

    protected def onCompanionRegistered(comp: SyncObjectCompanion[A]): Unit = ()

    def register(id             : NamedIdentifier,
                 rootObjectOwner: UniqueTag with NetworkFriendlyEngineTag,
                 creator        : SyncInstanceCreator[A],
                 mirror         : Boolean): SyncObjectCompanion[A] = {

        if (companions.contains(id))
            throw new ConnectedObjectAlreadyRegisteredException(s"Tree '$id' already registered.")

        val rootObjectOwnerNT = selector.retrieveNT(rootObjectOwner)
        val nodeReference     = ConnectedObjectReference(cache.family, cache.cacheID, rootObjectOwnerNT, true, id)

        val lock = NetworkObjectReferencesLocks.getLock(nodeReference)
        lock.lock()
        try {
            val choreographer = new InvocationChoreographer()
            val syncLevel     = if (mirror) Mirror else Synchronized //root objects can either be mirrored or fully sync objects.
            val context       = UsageConnectedObjectContext(
                rootObjectOwnerNT,
                creator.syncClassDef, syncLevel,
                choreographer, selector)

            val contract = cache.getContract(creator, context)
            val obj      = cache.defaultInstantiator.newSynchronizedInstance[A](creator)


            val chip        = ObjectChip[A](contract, selector, defaultPool, obj)
            val puppeteer   = ObjectPuppeteer[A](cache.channel, nodeReference)
            val presence    = registry.getPresence(nodeReference)
            val origin      = creator.getOrigin.map(new WeakReference(_))

            val baseData = new CompanionData[A](nodeReference, presence, rootObjectOwner, selector)
            val chipData = new ChippedObjectCompanionData[A](selector, chip, contract, choreographer, obj, registry.secondFloor)(baseData)
            val syncData = new SyncObjectCompanionData[A](puppeteer, obj, syncLevel, origin)(chipData)
            val node     = new SyncObjectCompanionImpl[A](syncData)
            registerComp(node: SyncObjectCompanion[A])
            node
        } finally lock.unlock()
    }
}
