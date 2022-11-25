package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncObjectFieldManipulation
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.contract.level.ConcreteSyncLevel.Synchronized
import fr.linkit.api.gnom.cache.sync.contract.level.MirrorableSyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, MirrorableSyncLevel, SyncLevel}
import fr.linkit.api.gnom.cache.sync.env.{ChippedObjectCompanion, ConnectedObjectCompanion, ObjectConnector, SyncObjectCompanion}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncObjectInstantiator
import fr.linkit.api.gnom.network.tag.{Current, EngineSelector, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.cache.sync.env.node._
import fr.linkit.engine.gnom.cache.sync.instantiation.{ContentSwitcher, MirroringInstanceCreator}

import java.util.concurrent.ThreadLocalRandom
import scala.annotation.switch

class SecondRegistryLayer(nph         : CORNPH,
                          selector    : EngineSelector,
                          defaultPool : Procrastinator,
                          channel     : RequestPacketChannel,
                          instantiator: SyncObjectInstantiator,
                          dataSupp    : ConnectedObjectDataSupplier,
                          cacheInfo   : CacheInfo)
        extends ConnectedObjectRegistryLayer[AnyRef](nph, selector, defaultPool, channel, instantiator, dataSupp, cacheInfo) with ObjectConnector {

    override def findCompanion(id: NamedIdentifier): Option[SyncObjectCompanion[AnyRef]] = companions.get(id)

    override def secondLayer: SecondRegistryLayer = this

    override def initObject(obj: ConnectedObject[AnyRef]): Unit = {

    }


    override def connectObject[B <: AnyRef](source : B,
                                            ownerID: UniqueTag with NetworkFriendlyEngineTag,
                                            kind   : SyncLevel,
                                            id     : Int): ConnectedObjectCompanion[B] = {
        (kind: @switch) match {
            case ConcreteSyncLevel.NotRegistered => throw new IllegalArgumentException("insertionKind = NotRegistered.")
            case Chipped | Synchronized | Mirror =>
                getOrGenConnectedObject[B](id, source, kind)(ownerID)
        }
    }

    override def makeMirroredObject[B <: AnyRef](classDef: SyncClassDef,
                                                 ownerID : UniqueTag with NetworkFriendlyEngineTag,
                                                 id      : Int): ConnectedObjectCompanion[B] = {
        val syncObject = instantiator.newSynchronizedInstance[B](new MirroringInstanceCreator[B](classDef))
        initSynchronizedObject[B](id, syncObject, ownerID, true)
    }

    private def getOrGenConnectedObject[B <: AnyRef](id    : NamedIdentifier,
                                                     source: B,
                                                     level : SyncLevel)(ownerID: UniqueTag with NetworkFriendlyEngineTag): ConnectedObjectCompanion[B] = {
        if (level == ConcreteSyncLevel.NotRegistered)
            throw new IllegalArgumentException("level = NotRegistered.")
        if (source == null)
            throw new NullPointerException("source object is null.")
        if (source.isInstanceOf[ConnectedObject[_]])
            throw new CannotConnectException("This object is already a connected object.")

        findCompanionFromOrigin(source) match {
            case Some(value: ConnectedObjectCompanion[B]) =>
                if (level != Synchronized && level != MirrorableSyncLevel.Mirror)
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a SynchronizedObject node")
                value
            case Some(value: ConnectedObjectCompanion[B]) =>
                if (!level.isInstanceOf[MirrorableSyncLevel])
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a Mirrorable object node")
                value
            case None                                     =>
                level match {
                    case Chipped               =>
                        newChippedObject(id, source)
                    case Synchronized | Mirror =>
                        val syncObject = instantiator.newSynchronizedInstance[B](new ContentSwitcher[B](source))
                        initSynchronizedObject[B](id, syncObject, ownerID, level == Mirror)
                }

        }
    }

    private def newChippedObject[B <: AnyRef](id     : NamedIdentifier,
                                              chipped: B): ChippedObjectCompanion[B] = {
        //if (ownerID != currentIdentifier)
        //    throw new IllegalConnectedObjectRegistration("Attempted to create a chipped object that is not owned by the current engine. Chipped Objects can only exists on their origin engines.")
        val adapter = new ChippedObjectAdapter[B](chipped)
        initChippedObject(id, adapter)
    }

    private def initChippedObject[B <: AnyRef](id     : NamedIdentifier,
                                               adapter: ChippedObjectAdapter[B]): ChippedObjectCompanion[B] = {
        val currentNT = selector.retrieveNT(Current)
        val data      = dataSupp.newNodeData(new ChippedObjectNodeDataRequest[B](id, currentNT, false, adapter))
        val node      = new ChippedObjectCompanionImpl[B](data)
        adapter.initialize(node)
        node
    }

    private def initSynchronizedObject[B <: AnyRef](id         : NamedIdentifier,
                                                    syncObject : B with SynchronizedObject[B],
                                                    ownerTag   : UniqueTag with NetworkFriendlyEngineTag,
                                                    isMirroring: Boolean): ConnectedObjectCompanion[B] = {
        if (syncObject.isInitialized)
            throw new ConnectedObjectAlreadyInitialisedException(s"Could not register synchronized object '${syncObject.getClass.getName}' : Object already initialized.")

        val level   = if (isMirroring) Mirror else Synchronized
        val ownerNT = selector.retrieveNT(ownerTag)
        val data    = dataSupp.newNodeData(new SyncNodeDataRequest[B](id, ownerNT, false, syncObject, level))
        val node    = new SyncObjectCompanionImpl[B](data)
        nph.registerReference(node.reference)

        scanSyncObjectFields(node, ownerTag, syncObject)
        node
    }

    @inline
    private def scanSyncObjectFields[B <: AnyRef](node      : SyncObjectCompanionImpl[B],
                                                  ownerID   : UniqueTag with NetworkFriendlyEngineTag,
                                                  syncObject: B with SynchronizedObject[B]): Unit = {
        val manipulation = new SyncObjectFieldManipulation {

            override def findConnectedVersion(origin: Any): Option[ConnectedObject[AnyRef]] = {
                findCompanionFromOrigin(cast(origin)).map(_.obj)
            }

            override def initObject(sync: ConnectedObject[AnyRef]): Unit = {
                val id = sync.reference.identifier
                sync match {
                    case sync: SynchronizedObject[AnyRef]         =>
                        initSynchronizedObject[AnyRef](id, sync, ownerID, false)
                    case chippedObj: ChippedObjectAdapter[AnyRef] =>
                        newChippedObject[AnyRef](id, chippedObj)
                }
            }

            override def createConnectedObject(obj: AnyRef, kind: SyncLevel): ConnectedObject[AnyRef] = {
                val id = ThreadLocalRandom.current().nextInt() /*forest.removeLinkedReference(obj)
                        .map(_.nodePath.last)
                        .getOrElse(NamedIdentifier(obj.getClass.getSimpleName, ))*/
                getOrGenConnectedObject(id, obj, kind)(ownerID).obj
            }
        }
        node.contract.applyFieldsContracts(syncObject, manipulation)
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]
    /*
        private def createUnknownObjectNode(path: Array[NamedIdentifier]): MutableNode[AnyRef] = {
            val parent = getParent(path.dropRight(1))
            val data   = dataFactory.newNodeData(new NormalNodeDataRequest[AnyRef](parent, path, null))
            val node   = new UnknownObjectSyncNode(data)
            parent.addChild(node)
            node
        }*/


}
