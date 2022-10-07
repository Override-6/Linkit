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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.application.resource.local.LocalFolder
import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.contract.{StructureContract, SyncLevel}
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncInstanceInstantiator, SyncObjectInstantiationException}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.tree.{ConnectedObjectNode, NoSuchSyncNodeException}
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.{CacheAttachHandler, CacheContentHandler}
import fr.linkit.api.gnom.cache.{SharedCacheFactory, SharedCacheReference}
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, Submitter}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.AbstractSharedCache
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, EmptyContractDescriptorData}
import fr.linkit.engine.gnom.cache.sync.generation.{DefaultSyncClassCenter, SyncClassStorageResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.cache.sync.invokation.UsageConnectedObjectContext
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.gnom.cache.sync.tree._
import fr.linkit.engine.gnom.cache.sync.tree.node._
import fr.linkit.engine.gnom.packet.fundamental.{EmptyPacket, RefPacket}
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{AnyRefPacket, ObjectPacket}
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.gnom.packet.traffic.{AbstractPacketTraffic, ChannelScopes}
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks
import fr.linkit.engine.internal.debug.{ConnectedObjectCreationStep, ConnectedObjectTreeRetrievalStep, Debugger, RequestStep}

import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class DefaultConnectedObjectCache[A <: AnyRef] protected(channel                      : CachePacketChannel,
                                                         classCenter                  : SyncClassCenter,
                                                         override val defaultContracts: ContractDescriptorData,
                                                         override val network         : Network)
    extends AbstractSharedCache(channel) with InternalConnectedObjectCache[A] {

    private  val currentIdentifier: String                     = channel.traffic.connection.currentIdentifier
    override val forest           : DefaultSyncObjectForest[A] = {
        val omc = network.connection.traffic match {
            case traffic: AbstractPacketTraffic => traffic.getObjectManagementChannel
            case _                              => throw new UnsupportedOperationException("Cannot retrieve object management channel.")
        }
        new DefaultSyncObjectForest[A](this, channel.manager.getCachesLinker, omc)
    }
    channel.setHandler(CacheCenterHandler)

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A], contracts: ContractDescriptorData): A with SynchronizedObject[A] = {
        AppLoggers.ConnObj.debug(s"Creating connected object at $reference with id $id.")
        makeSyncObject(id, creator, contracts, false)
    }

    override def mirrorObject(id: Int, creator: SyncInstanceCreator[_ <: A], contracts: ContractDescriptorData): A with SynchronizedObject[A] = {
        AppLoggers.ConnObj.debug(s"Creating mirrored object at $reference with id $id.")
        makeSyncObject(id, creator, contracts, true)
    }

    private def makeSyncObject(id: Int, creator: SyncInstanceCreator[_ <: A], contracts: ContractDescriptorData, mirror: Boolean): A with SynchronizedObject[A] = {
        val treeID = NamedIdentifier(creator.syncClassDef.mainClass.getSimpleName, id)
        Debugger.push(ConnectedObjectCreationStep(ConnectedObjectReference(family, cacheID, currentIdentifier, Array(treeID))))
        val tree        = createNewTree(treeID, currentIdentifier, creator.asInstanceOf[SyncInstanceCreator[A]], mirror, contracts)
        val treeProfile = ObjectTreeProfile(treeID, tree.getRoot.obj, currentIdentifier, mirror, contracts)
        AppLoggers.ConnObj.debug(s"Notifying other caches located on '$reference' that a new connected object has been added on the cache.")
        channel.makeRequest(ChannelScopes.include(ownerID))
               .addPacket(ObjectPacket(treeProfile))
               .submit()
        //Indicate that a new object has been posted.
        val node = tree.getRoot
        Debugger.pop()
        node.obj
    }

    private def newChippedObjectData[B <: AnyRef](level: SyncLevel, req: ChippedObjectNodeDataRequest[B]): ChippedObjectNodeData[B] = {
        import req._
        val tree            = parent.tree.asInstanceOf[DefaultConnectedObjectTree[B]]
        val originClass     = chippedObject.getClassDef.mainClass.asInstanceOf[Class[B]]
        val path            = parent.nodePath :+ id
        val contractFactory = tree.contractFactory
        val choreographer   = new InvocationChoreographer()
        val reference       = ConnectedObjectReference(family, cacheID, req.ownerID, path)
        val presence        = forest.getPresence(reference)
        val context         = UsageConnectedObjectContext(req.ownerID, tree.rootNode.ownerID, currentIdentifier, this.ownerID, chippedObject.getClassDef, level, choreographer)
        val contract        = contractFactory.getContract[B](originClass, context)
        val chip            = ObjectChip[B](contract, network, chippedObject)
        new ChippedObjectNodeData[B](
            network, chip, contract, choreographer, chippedObject,
            )(new NodeData[B](reference, presence, tree, currentIdentifier, req.ownerID, Some(parent)))
    }

    private def newSyncObjectData[B <: AnyRef](req: SyncNodeDataRequest[B]): SyncObjectNodeData[B] = {
        val chippedData = newChippedObjectData[B](req.syncLevel, req)
        val puppeteer   = new ObjectPuppeteer[B](channel, this, chippedData.reference)
        val originRef   = req.origin.map(new WeakReference[B](_))
        new SyncObjectNodeData[B](puppeteer, req.syncObject, req.syncLevel, originRef)(chippedData)
    }

    private def newRegularNodeData[B <: AnyRef](req: NormalNodeDataRequest[B]): NodeData[B] = {
        import req._
        val reference = new ConnectedObjectReference(family, cacheID, req.ownerID, path)
        val presence  = forest.getPresence(reference)
        val tree      = parent.tree.asInstanceOf[DefaultConnectedObjectTree[_]] //TODO REMOVE THIS CAST
        new NodeData[B](reference, presence, tree, currentIdentifier, req.ownerID, Some(parent))
    }

    override def newNodeData[B <: AnyRef, N <: NodeData[B]](req: NodeDataRequest[B, N]): N = {
        implicit def cast[X](a: Any) = a.asInstanceOf[X]

        req match {
            case req: NormalNodeDataRequest[B]        => newRegularNodeData[B](req)
            case req: SyncNodeDataRequest[B]          => newSyncObjectData[B](req)
            case req: ChippedObjectNodeDataRequest[B] => newChippedObjectData[B](SyncLevel.Chipped, req)
        }
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        forest.findTree(id).map(_.rootNode.obj)
    }

    private def precompileClasses(data: ContractDescriptorData): Unit = data match {
        case data: ContractDescriptorDataImpl if !data.isPrecompiled =>
            data.precompile(classCenter)
        case _                                                       =>
    }

    protected def getRootContract(factory: SyncObjectContractFactory)(creator: SyncInstanceCreator[A],
                                                                      context: ConnectedObjectContext): StructureContract[A] = {
        factory.getContract[A](creator.syncClassDef.mainClass.asInstanceOf[Class[A]], context)
    }

    private def createNewTree(id             : NamedIdentifier,
                              rootObjectOwner: String,
                              creator        : SyncInstanceCreator[A],
                              mirror         : Boolean,
                              contracts      : ContractDescriptorData = defaultContracts): DefaultConnectedObjectTree[A] = {
        precompileClasses(contracts)

        if (forest.findTreeLocal(id).isDefined)
            throw new ObjectTreeAlreadyRegisteredException(s"Tree '$id' already registered.")

        val nodeReference = ConnectedObjectReference(family, cacheID, rootObjectOwner, Array(id))

        val lock = NetworkObjectReferencesLocks.getLock(nodeReference)
        lock.lock()
        try {
            val choreographer = new InvocationChoreographer()
            val syncLevel     = if (mirror) Mirror else Synchronized //root objects can either be mirrored or fully sync objects.
            val context       = UsageConnectedObjectContext(
                rootObjectOwner, rootObjectOwner,
                currentIdentifier, this.ownerID,
                creator.syncClassDef, syncLevel,
                choreographer)
            val factory       = SyncObjectContractFactory(contracts)

            val rootContract = getRootContract(factory)(creator, context)
            val root         = DefaultInstantiator.newSynchronizedInstance[A](creator)

            val chip             = ObjectChip[A](rootContract, network, root)
            val puppeteer        = ObjectPuppeteer[A](channel, this, nodeReference)
            val presence         = forest.getPresence(nodeReference)
            val origin           = creator.getOrigin.map(new WeakReference(_))
            val rootNodeSupplier = (tree: DefaultConnectedObjectTree[A]) => {
                val baseData = new NodeData[A](nodeReference, presence, tree, currentIdentifier, rootObjectOwner, None)
                val chipData = new ChippedObjectNodeData[A](network, chip, rootContract, choreographer, root)(baseData)
                val syncData = new SyncObjectNodeData[A](puppeteer, root, syncLevel, origin)(chipData)
                new RootObjectNodeImpl[A](syncData)
            }

            val tree = new DefaultConnectedObjectTree[A](currentIdentifier, network, forest, id, DefaultInstantiator, this, factory)(rootNodeSupplier)
            forest.addTree(id, tree)
            if (forest.isRegisteredAsUnknown(id)) {
                forest.transferUnknownTree(id)
            }

            tree
        } finally lock.unlock()
    }

    private def handleInvocationPacket(ip: InvocationPacket, bundle: RequestPacketBundle): Unit = {
        val ref = ip.objRef
        if (ref.family != family || ref.cacheID != cacheID)
            throw new IllegalArgumentException(s"Invocation Packet's targeted object reference is not contained in this cache. (targeted: $ref, current cache location is $reference)")
        val path = ref.nodePath
        val lock = NetworkObjectReferencesLocks.getLock(ref)

        lock.lock()
        val node = try findNode(path) finally lock.unlock()

        val senderID = bundle.coords.senderID
        AppLoggers.COInv.trace(s"Handling invocation packet over object ${ip.objRef}. For method with id '${ip.methodID}', expected engine identifier return : '${ip.expectedEngineIDReturn}'")
        node.fold(AppLoggers.ConnObj.error(s"Could not find sync object node for connected object located at $ref")) {
            case node: TrafficInterestedNode[_] => node.handlePacket(ip, senderID, bundle.responseSubmitter)
            case _                              =>
                throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedNode[_]].getSimpleName} in order to handle a member rmi request.")
        }
    }

    private def findNode(path: Array[NamedIdentifier]): Option[ConnectedObjectNode[A]] = {
        forest
            .findTree(path.head)
            .flatMap(tree => {
                if (path.length == 1)
                    Some(tree.rootNode)
                else
                    tree.findNode[A](path)
            })
    }

    private def handleNewTree(profile: ObjectTreeProfile[A]): Unit = if (forest.findTreeLocal(profile.treeID).isEmpty) {
        import profile._
        createNewTree(treeID, treeOwner, new InstanceWrapper[A](rootObject), mirror, contracts)
    }

    override def isRegistered(id: NamedIdentifier): Boolean = {
        forest.findTreeLocal(id).isDefined || {
            forest.isPresentOnEngine(ownerID, ConnectedObjectReference(family, cacheID, ownerID, Array(id)))
        }
    }

    private final val treeRequestsLocks = mutable.HashMap.empty[NamedIdentifier, ReentrantLock]

    override def requestTree(id: NamedIdentifier): Unit = {
        val treeRef = ConnectedObjectReference(family, cacheID, ownerID, Array(id))


        val refLock         = NetworkObjectReferencesLocks.getLock(treeRef)
        val isSyncOnRefLock = refLock.isHeldByCurrentThread
        val requestLock     = treeRequestsLocks.getOrElseUpdate(id, new ReentrantLock())
        if (requestLock.isLocked) {
            //a request for the same object is already pending from another thread, just wait for it to end the request.
            refLock.unlock()
            requestLock.lock() //lock to wait
            requestLock.unlock() //then directly release
            if (isSyncOnRefLock)
                refLock.lock()
            return //directly return : the request was already done by another thread
        }
        if (!forest.isPresentOnEngine(ownerID, treeRef)) {
            return
        }
        requestLock.lock()
        forest.putUnknownTree(id)

        Debugger.push(ConnectedObjectTreeRetrievalStep(treeRef))
        AppLoggers.ConnObj.trace(s" Requesting root object $treeRef.")

        if (isSyncOnRefLock) refLock.unlock()
        try {
            Debugger.push(RequestStep("CO tree retrieval", s"retrieve tree $treeRef", ownerID, channel.reference))
            val response = channel.makeRequest(ChannelScopes.include(ownerID))
                                  .addPacket(AnyRefPacket(id))
                                  .submit()
                                  .nextResponse
            Debugger.pop()
            response.nextPacket[Packet] match {
                case ObjectPacket(profile: ObjectTreeProfile[A]) =>
                    handleNewTree(profile)
                case EmptyPacket                                 => //the tree does not exists, do nothing.
            }
        } finally {
            Debugger.pop()
            treeRequestsLocks -= id
            requestLock.unlock()
            if (isSyncOnRefLock)
                refLock.lock() //go back to initial state for the reference's lock
        }
    }

    private def handleTreeRetrieval(id: NamedIdentifier, response: Submitter[Unit]): Unit = {
        forest.findTreeInternal(id) match {
            case Some(tree) =>
                val node        = tree.rootNode
                val treeProfile = ObjectTreeProfile(id, node.obj, currentIdentifier, node.isMirrored, tree.contractFactory.data)
                response.addPacket(ObjectPacket(treeProfile)).submit()
            case None       =>
                response.addPacket(EmptyPacket).submit()
        }
    }

    private object DefaultInstantiator extends SyncInstanceInstantiator {

        override def newSynchronizedInstance[B <: AnyRef](creator: SyncInstanceCreator[B]): B with SynchronizedObject[B] = {
            val syncClass = classCenter.getSyncClass[B](creator.syncClassDef)
            try {
                creator.getInstance(syncClass)
            } catch {
                case NonFatal(e) =>
                    throw new SyncObjectInstantiationException(e.getMessage +
                                                                   s"""\nMaybe the origin class of generated sync class '${syncClass.getName}' has been modified ?
                                                                      | Try to regenerate the sync class of ${creator.syncClassDef.mainClass}.
                                                                      | """.stripMargin, e)
            }
        }

    }

    private object CacheCenterHandler extends CacheContentHandler[CacheRepoContent[A]] with CacheAttachHandler {

        override val lazyContentHandling: Boolean = true

        override def getInitialContent: CacheRepoContent[A] = CacheRepoContent(Array())

        override def getContent: CacheRepoContent[A] = forest.snapshotContent

        override def initializeContent(content: CacheRepoContent[A]): Unit = {
            val array = content.array
            if (array.isEmpty)
                return

            array.foreach(profile => {
                val rootObject = profile.rootObject
                val owner      = profile.treeOwner
                val treeID     = profile.treeID
                val mirror     = profile.mirror
                val contracts  = profile.contracts
                //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
                if (isRegistered(treeID)) {
                    forest.findTreeInternal(treeID).map(_.getRoot).fold {
                        throw new NoSuchSyncNodeException(s"Could not find root object's chip ${rootObject.reference}")
                    }(_.chip.updateObject(rootObject))
                }
                //it's an object that must be remotely controlled because it is chipped by another objects cache.
                createNewTree(treeID, owner, new InstanceWrapper[A](rootObject), mirror, contracts)

            })
        }

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket                        =>
                    handleInvocationPacket(ip, bundle)
                case AnyRefPacket(id: NamedIdentifier)           =>
                    handleTreeRetrieval(id, bundle.responseSubmitter)
                case ObjectPacket(profile: ObjectTreeProfile[A]) =>
                    handleNewTree(profile)
            }
        }

        override def onEngineAttached(engine: Engine): Unit = {
            AppLoggers.ConnObj.debug(s"Engine ${engine} attached to this synchronized object cache")
        }

        override def onEngineDetached(engine: Engine): Unit = {
            AppLoggers.ConnObj.debug(s"Engine ${engine} detached to this synchronized object cache")
        }

        override val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH] = Some(forest)
    }

}

object DefaultConnectedObjectCache extends ConnectedObjectCacheFactories {

    import SharedCacheFactory.lambdaToFactory

    //value does not have any meaning

    private final val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir") + "/Classes"

    override def apply[A <: AnyRef : ClassTag]: SharedCacheFactory[ConnectedObjectCache[A]] = {
        apply[A](EmptyContractDescriptorData)
    }

    override def apply[A <: AnyRef : ClassTag](contract: ContractDescriptorData): SharedCacheFactory[ConnectedObjectCache[A]] = {
        lambdaToFactory(classOf[DefaultConnectedObjectCache[A]])(channel => {
            apply[A](channel, contract, channel.traffic.connection.network)
        })
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](network: Network): SharedCacheFactory[ConnectedObjectCache[A]] = {
        apply[A](null, network)
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](contract: ContractDescriptorData, network: Network): SharedCacheFactory[ConnectedObjectCache[A]] = {
        lambdaToFactory(classOf[DefaultConnectedObjectCache[A]])(channel => {
            apply[A](channel, contract, network)
        })
    }

    private def apply[A <: AnyRef : ClassTag](channel: CachePacketChannel, contracts: ContractDescriptorData, network: Network): ConnectedObjectCache[A] = {
        val app       = channel.manager.network.connection.getApp
        val resources = app.getAppResources.getOrOpen[LocalFolder](ClassesResourceDirectory)
                           .getEntry
                           .getOrAttachRepresentation[SyncClassStorageResource]("lambdas")
        val generator = new DefaultSyncClassCenter(resources, app.compilerCenter)

        new DefaultConnectedObjectCache[A](channel, generator, contracts, network)
    }

    case class ObjectTreeProfile[A <: AnyRef](treeID    : NamedIdentifier,
                                              rootObject: A with SynchronizedObject[A],
                                              treeOwner : String,
                                              mirror    : Boolean,
                                              contracts : ContractDescriptorData) extends Serializable
}