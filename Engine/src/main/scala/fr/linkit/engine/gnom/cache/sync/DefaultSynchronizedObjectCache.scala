/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.RegistrationKind._
import fr.linkit.api.gnom.cache.sync.contract.behavior.SyncObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.{RegistrationKind, StructureContract}
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncInstanceInstantiator, SyncObjectInstantiationException}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.tree.{ConnectedObjectNode, NoSuchSyncNodeException}
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.{AttachHandler, CacheHandler, ContentHandler}
import fr.linkit.api.gnom.cache.{SharedCacheFactory, SharedCacheReference}
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.reference.linker.NetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.TrafficInterestedNPH
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.AbstractSharedCache
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription.isNotOverridable
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, EmptyContractDescriptorData}
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.cache.sync.invokation.UsageSyncObjectContext
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.gnom.cache.sync.tree._
import fr.linkit.engine.gnom.cache.sync.tree.node._
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes

import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class DefaultSynchronizedObjectCache[A <: AnyRef] protected(channel: CachePacketChannel,
                                                                       classCenter: SyncClassCenter,
                                                                       override val defaultContracts: ContractDescriptorData,
                                                                       override val network: Network)
        extends AbstractSharedCache(channel) with InternalSynchronizedObjectCache[A] {

    private  val cacheOwnerId     : String                     = channel.manager.ownerID
    private  val currentIdentifier: String                     = channel.traffic.connection.currentIdentifier
    override val forest           : DefaultSyncObjectForest[A] = new DefaultSyncObjectForest[A](this, channel.manager.getCachesLinker, network.objectManagementChannel)
    channel.setHandler(CenterHandler)

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A] = {
        syncObject(id, creator, defaultContracts)
    }

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A], contracts: ContractDescriptorData): A with SynchronizedObject[A] = {
        val tree        = createNewTree(id, currentIdentifier, creator.asInstanceOf[SyncInstanceCreator[A]], contracts)
        val treeProfile = ObjectTreeProfile(id, tree.getRoot.obj, currentIdentifier, contracts)
        channel.makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket(treeProfile))
                .putAllAttributes(this)
                .submit()
        //Indicate that a new object has been posted.
        val wrapperNode = tree.getRoot
        wrapperNode.obj
    }

    override def makeTree(root: SynchronizedObject[A]): Unit = {
        val reference = root.reference
        val path      = reference.nodePath
        if (path.length != 1)
            throw new IllegalArgumentException("Can only make tree from a root synchronised object.")
        val wrapper = new InstanceWrapper[A](root.asInstanceOf[A with SynchronizedObject[A]])
        createNewTree(path.head, reference.ownerID, wrapper)
    }

    override def newChippedObjectData[B <: AnyRef](parent: MutableNode[_ <: AnyRef], id: Int, chippedObject: ChippedObject[B],
                                                   ownerID: String): ChippedObjectNodeData[B] = {
        val tree          = parent.tree.asInstanceOf[DefaultSynchronizedObjectTree[B]]
        val originClass   = chippedObject.getConnectedObjectClass
        val path          = parent.treePath :+ id
        val behaviorStore = tree.contractFactory
        val choreographer = new InvocationChoreographer()
        val context       = UsageSyncObjectContext(ownerID, ownerID, currentIdentifier, cacheOwnerId, choreographer)
        val contract      = behaviorStore.getObjectContract[B](originClass, context)
        val chip          = ObjectChip[B](contract, network, chippedObject)
        val reference     = new ConnectedObjectReference(family, cacheID, ownerID, path)
        val presence      = forest.getPresence(reference)
        new ChippedObjectNodeData[B](
            network, chip, contract, choreographer, chippedObject,
        )(new NodeData[B](reference, presence, tree, currentIdentifier, ownerID, Some(parent)))
    }

    override def newSyncObjectData[B <: AnyRef](parent: MutableNode[_ <: AnyRef],
                                                id: Int,
                                                syncObject: B with SynchronizedObject[B],
                                                origin: Option[B],
                                                ownerID: String): SyncObjectNodeData[B] = {
        val chippedData = newChippedObjectData[B](parent, id, syncObject, ownerID)
        val puppeteer   = new ObjectPuppeteer[B](channel, this, chippedData.reference)
        val originRef   = origin.map(new WeakReference[B](_))
        new SyncObjectNodeData[B](puppeteer, syncObject, originRef)(chippedData)
    }

    override def newUnknownObjectData[B <: AnyRef](parent: MutableNode[_ <: AnyRef], path: Array[Int]): NodeData[B] = {
        val reference = new ConnectedObjectReference(family, cacheID, null, path)
        val presence  = forest.getPresence(reference)
        val tree      = parent.tree.asInstanceOf[DefaultSynchronizedObjectTree[_]] //TODO REMOVE THIS CAST
        new NodeData[B](reference, presence, tree, currentIdentifier, null, Some(parent))
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        forest.findTree(id).map(_.rootNode.obj)
    }

    private def precompileClasses(data: ContractDescriptorData): Unit = data match {
        case data: ContractDescriptorDataImpl if !data.isPrecompiled() =>
            precompileClasses(data.descriptors)
            data.markAsPrecompiled()
        case _                                                         =>
    }

    private def precompileClasses(descs: Array[StructureContractDescriptor[_]]): Unit = {
        var classes = mutable.HashSet.empty[Class[_]]

        def addClass(clazz: Class[_]): Unit = {
            if (!Modifier.isAbstract(clazz.getModifiers))
                classes += clazz
        }

        descs.foreach(desc => {
            val clazz         = desc.targetClass
            val mirroringInfo = desc.mirroringInfo
            if (mirroringInfo.isDefined)
                classes += mirroringInfo.get.stubClass
            else addClass(clazz)
            desc.fields.filter(_.registrationKind == Synchronized).foreach(f => addClass(f.desc.javaField.getType))
            desc.methods.foreach(method => {
                val javaMethod = method.description.javaMethod
                if (method.returnValueContract.exists(_.registrationKind == Synchronized))
                    addClass(javaMethod.getReturnType)
                val paramsContracts = method.parameterContracts
                if (paramsContracts.nonEmpty)
                    paramsContracts.zip(javaMethod.getParameterTypes)
                            .foreach { case (desc, paramType) => if (desc.registrationKind == Synchronized) addClass(paramType) }
            })
        })
        classes -= classOf[Object]
        classes = classes.filterNot(classCenter.isClassGenerated).filterNot(c => isNotOverridable(c.getModifiers))
        if (classes.isEmpty)
            return
        AppLogger.info(s"Found ${classes.size} classes to compile in their sync versions")
        AppLogger.debug("Classes to compile :")
        classes.foreach(clazz => AppLogger.debug(s"\tgen.${clazz}Sync"))
        classCenter.preGenerateClasses(classes.toList)
    }

    private def isObjectPresent(location: ConnectedObjectReference): Boolean = {
        (location.cacheID == cacheID) && location.family == family && {
            val path = location.nodePath
            forest.findTree(path.head).exists(_.findNode(path).isDefined)
        }
    }

    protected def getRootContract(factory: SyncObjectContractFactory)(creator: SyncInstanceCreator[A], context: SyncObjectContext): StructureContract[A] = {
        factory.getObjectContract[A](creator.tpeClass, context)
    }

    private def createNewTree(id: Int, rootObjectOwner: String,
                              creator: SyncInstanceCreator[A],
                              contracts: ContractDescriptorData = defaultContracts): DefaultSynchronizedObjectTree[A] = {
        precompileClasses(contracts)

        val nodeReference = ConnectedObjectReference(family, cacheID, rootObjectOwner, Array(id))
        val choreographer = new InvocationChoreographer()
        val context       = UsageSyncObjectContext(rootObjectOwner, rootObjectOwner, currentIdentifier, cacheOwnerId, choreographer)
        val factory       = SyncObjectContractFactory(contracts)

        val rootContract = getRootContract(factory)(creator, context)
        val root         = DefaultInstantiator.newSynchronizedInstance[A](creator)

        val chip      = ObjectChip[A](rootContract, network, root)
        val puppeteer = ObjectPuppeteer[A](channel, this, nodeReference)
        val presence  = forest.getPresence(nodeReference)
        val origin    = creator.getOrigin.map(new WeakReference(_))
        val rootNode  = (tree: DefaultSynchronizedObjectTree[A]) => {
            val regularData = new NodeData[A](nodeReference, presence, tree, currentIdentifier, rootObjectOwner, None)
            val chipData    = new ChippedObjectNodeData[A](network, chip, rootContract, choreographer, root)(regularData)
            val syncData    = new SyncObjectNodeData[A](puppeteer, root, origin)(chipData)
            new RootObjectNodeImpl[A](syncData)
        }
        val tree      = new DefaultSynchronizedObjectTree[A](currentIdentifier, network, forest, id, DefaultInstantiator, this, factory)(rootNode)
        forest.addTree(id, tree)
        tree
    }

    private def handleInvocationPacket(ip: InvocationPacket, bundle: RequestPacketBundle): Unit = {
        val path     = ip.path
        val node     = findNode(path)
        val senderID = bundle.coords.senderID
        node.fold(AppLogger.error(s"Could not find sync object node for synchronised object located at $reference/~${path.mkString("/")}")) {
            case node: TrafficInterestedNode[_] => node.handlePacket(ip, senderID, bundle.responseSubmitter)
            case _                              =>
                throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedNode[_]].getSimpleName} in order to handle a member rmi request.")
        }
    }

    private def findNode(path: Array[Int]): Option[ConnectedObjectNode[A]] = {
        forest
                .findTree(path.head)
                .flatMap(tree => {
                    if (path.length == 1)
                        Some(tree.rootNode)
                    else
                        tree.findNode[A](path)
                })
    }

    private def handleNewObject(treeID: Int,
                                rootObject: AnyRef with SynchronizedObject[AnyRef],
                                owner: String,
                                contracts: ContractDescriptorData): Unit = {
        if (!isRegistered(treeID)) {
            createNewTree(treeID, owner, new InstanceWrapper[A](rootObject.asInstanceOf[A with SynchronizedObject[A]]), contracts)
        }
    }

    override def content: List[A with SynchronizedObject[A]] = {
        forest.snapshotContent.array.map(_.rootObject).toList
    }

    override def isRegistered(id: Int): Boolean = {
        forest.findTree(id).isDefined
    }


    private object DefaultInstantiator extends SyncInstanceInstantiator {

        override def newSynchronizedInstance[B <: AnyRef](creator: SyncInstanceCreator[B]): B with SynchronizedObject[B] = {
            val syncClass = classCenter.getSyncClass[B](creator.tpeClass)
            try {
                creator.getInstance(syncClass)
            } catch {
                case NonFatal(e) =>
                    throw new SyncObjectInstantiationException(e.getMessage +
                            s"""\nMaybe the origin class of generated sync class '${syncClass.getName}' has been modified ?
                               | Try to regenerate the sync class of ${creator.tpeClass}.
                               | """.stripMargin, e)
            }
        }

    }

    private object CenterHandler extends CacheHandler with ContentHandler[CacheRepoContent[A]] with AttachHandler {

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket                             =>
                    handleInvocationPacket(ip, bundle)
                case ObjectPacket(location: ConnectedObjectReference) =>
                    bundle.responseSubmitter
                            .addPacket(BooleanPacket(isObjectPresent(location)))
                            .submit()

                case ObjectPacket(ObjectTreeProfile(treeID, rootObject: AnyRef with SynchronizedObject[AnyRef], owner, contracts)) =>
                    handleNewObject(treeID, rootObject, owner, contracts)
            }
        }

        override def initializeContent(content: CacheRepoContent[A]): Unit = {
            val array = content.array
            if (array.isEmpty)
                return

            array.foreach(profile => {
                val rootObject = profile.rootObject
                val owner      = profile.treeOwner
                val treeID     = profile.treeID
                val contracts  = profile.contracts
                //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
                if (isRegistered(treeID)) {
                    forest.findTreeInternal(treeID).map(_.getRoot).fold {
                        throw new NoSuchSyncNodeException(s"Could not find root object's chip ${rootObject.reference}")
                    }(_.chip.updateObject(rootObject))
                }
                //it's an object that must be remotely controlled because it is chipped by another objects cache.
                createNewTree(treeID, owner, new InstanceWrapper[A](rootObject), contracts)
                if (forest.isRegisteredAsUnknown(treeID)) {
                    forest.transferUnknownTree(treeID)
                }

            })
        }

        override def getContent: CacheRepoContent[A] = forest.snapshotContent

        override def onEngineAttached(engine: Engine): Unit = {
            AppLogger.debug(s"Engine ${engine.identifier} attached to this synchronized object cache !")
        }

        override def inspectEngine(engine: Engine, requestedCacheType: Class[_]): Option[String] = {
            val clazz = classOf[DefaultSynchronizedObjectCache[A]]
            if (requestedCacheType eq clazz)
                None
            else Some(s"Requested cache class is not ${clazz.getName} (received: ${requestedCacheType.getName}).")
        }

        override def onEngineDetached(engine: Engine): Unit = {
            AppLogger.debug(s"Engine ${engine.identifier} detached to this synchronized object cache !")
        }

        override val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH] = Some(forest)
    }

}

object DefaultSynchronizedObjectCache {

    private final val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    implicit def default[A <: AnyRef : ClassTag]: SharedCacheFactory[SynchronizedObjectCache[A]] = apply

    implicit def apply[A <: AnyRef : ClassTag]: SharedCacheFactory[SynchronizedObjectCache[A]] = {
        apply[A](EmptyContractDescriptorData)
    }

    def apply[A <: AnyRef : ClassTag](network: Network): SharedCacheFactory[SynchronizedObjectCache[A]] = {
        apply[A](null, network)
    }

    implicit def apply[A <: AnyRef : ClassTag](contracts: ContractDescriptorData): SharedCacheFactory[SynchronizedObjectCache[A]] = {
        channel => {
            apply[A](channel, contracts, channel.traffic.connection.network)
        }
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](contracts: ContractDescriptorData, network: Network): SharedCacheFactory[SynchronizedObjectCache[A]] = {
        channel => {
            apply[A](channel, contracts, network)
        }
    }

    private def apply[A <: AnyRef : ClassTag](channel: CachePacketChannel, contracts: ContractDescriptorData, network: Network): SynchronizedObjectCache[A] = {
        import fr.linkit.engine.application.resource.external.LocalResourceFolder._
        val context   = channel.manager.network.connection.getApp
        val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val generator = new DefaultSyncClassCenter(context.compilerCenter, resources)

        new DefaultSynchronizedObjectCache[A](channel, generator, contracts, network)
    }

    case class ObjectTreeProfile[A <: AnyRef](treeID: Int,
                                              rootObject: A with SynchronizedObject[A],
                                              treeOwner: String,
                                              contracts: ContractDescriptorData) extends Serializable



}