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
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncInstanceInstantiator}
import fr.linkit.api.gnom.cache.sync.tree.{NoSuchSyncNodeException, SyncNode, SyncObjectReference}
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.{AttachHandler, CacheHandler, ContentHandler}
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheFactory, SharedCacheReference}
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.reference.NetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.TrafficInterestedNPH
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.AbstractSharedCache
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.behavior.{AnnotationBasedMemberBehaviorFactory, DefaultObjectBehaviorStore}
import fr.linkit.engine.gnom.cache.sync.generation.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.gnom.cache.sync.tree._
import fr.linkit.engine.gnom.cache.sync.tree.node._
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.LinkitApplication

import scala.reflect.ClassTag

final class DefaultSynchronizedObjectCache[A <: AnyRef] private(channel: CachePacketChannel,
                                                                generator: SyncClassCenter,
                                                                override val defaultTreeViewBehavior: ObjectBehaviorStore,
                                                                override val network: Network)
    extends AbstractSharedCache(channel) with InternalSynchronizedObjectCache[A] {

    private  val currentIdentifier: String                     = channel.traffic.connection.currentIdentifier
    override val treeCenter       : DefaultObjectTreeCenter[A] = new DefaultObjectTreeCenter[A](this, network.objectManagementChannel)
    channel.setHandler(CenterHandler)

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A] = {
        syncObject(id, creator, defaultTreeViewBehavior)
    }

    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A], store: ObjectBehaviorStore): A with SynchronizedObject[A] = {
        val tree = createNewTree(id, currentIdentifier, creator.asInstanceOf[SyncInstanceCreator[A]], store)
        channel.makeRequest(ChannelScopes.discardCurrent)
            .addPacket(ObjectPacket(ObjectTreeProfile(id, tree.getRoot.synchronizedObject, currentIdentifier)))
            .putAllAttributes(this)
            .submit()
        //Indicate that a new object has been posted.
        val wrapperNode = tree.getRoot
        wrapperNode.synchronizedObject
    }

    private def createNewTree(id: Int, rootObjectOwner: String, creator: SyncInstanceCreator[A], behaviorTree: ObjectBehaviorStore = defaultTreeViewBehavior): DefaultSynchronizedObjectTree[A] = {
        val nodeLocation = new SyncObjectReference(family, cacheID, rootObjectOwner, Array(id))
        val rootBehavior = behaviorTree.getFromClass[A](creator.tpeClass)
        val root         = DefaultInstantiator.newWrapper[A](creator)
        val chip         = ObjectChip[A](rootBehavior, network, root)
        val puppeteer    = new ObjectPuppeteer[A](channel, this, nodeLocation, rootBehavior)
        val presence     = treeCenter.getPresence(nodeLocation)
        val rootNode     = (tree: DefaultSynchronizedObjectTree[A]) => {
            val data = new ObjectNodeData[A](puppeteer, chip, tree, nodeLocation, presence, root, currentIdentifier)
            new RootObjectSyncNode[A](data)
        }
        val tree         = new DefaultSynchronizedObjectTree[A](currentIdentifier, network, treeCenter, id, DefaultInstantiator, this, behaviorTree)(rootNode)
        treeCenter.addTree(id, tree)
        tree
    }


    override def makeTree(root: SynchronizedObject[A]): Unit = {
        val reference = root.reference
        val path = reference.nodePath
        if (path.length != 1)
            throw new IllegalArgumentException("Can only make tree from a root synchronised object.")
        val wrapper = new InstanceWrapper[A](root.asInstanceOf[A with SynchronizedObject[A]])
        createNewTree(path.head, reference.owner, wrapper)
    }

    override def newData[B <: AnyRef](parent: ObjectSyncNode[_],
                                      id: Int,
                                      syncObject: B with SynchronizedObject[B],
                                      ownerID: String): ObjectNodeData[B] = {
        val tree          = parent.tree
        val path          = parent.treePath :+ id
        val behaviorStore = tree.behaviorStore
        val behavior      = behaviorStore.getFromClass[B](syncObject.getSuperClass)
        val chip          = ObjectChip[B](behavior, network, syncObject)
        val reference     = new SyncObjectReference(family, cacheID, ownerID, path)
        val puppeteer     = new ObjectPuppeteer[B](channel, this, reference, behavior)
        val presence      = treeCenter.getPresence(reference)
        new ObjectNodeData[B](puppeteer, chip, tree, reference, presence, syncObject, currentIdentifier)
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        treeCenter.findTree(id).map(_.rootNode.synchronizedObject)
    }

    def isObjectPresent(location: SyncObjectReference): Boolean = {
        (location.cacheID == cacheID) && location.family == family && {
            val path = location.nodePath
            treeCenter.findTree(path.head).exists(_.findNode(path).isDefined)
        }
    }

    private def handleInvocationPacket(ip: InvocationPacket, bundle: RequestPacketBundle): Unit = {
        val path     = ip.path
        val node     = findNode(path)
        val senderID = bundle.coords.senderID
        node.fold(AppLogger.error(s"Could not find rootObject node at path ${path.mkString("/")}")) {
            case node: TrafficInterestedSyncNode[_] => node.handlePacket(ip, senderID, bundle.responseSubmitter)
            case _                                  =>
                throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedSyncNode[_]].getSimpleName} in order to handle a member rmi request.")
        }
    }

    private def findNode(path: Array[Int]): Option[SyncNode[A]] = {
        treeCenter
            .findTree(path.head)
            .flatMap(tree => {
                if (path.length == 1)
                    Some(tree.rootNode)
                else
                    tree.findNode[A](path)
            })
    }

    private def handleRootObjectPacket(treeID: Int, rootObject: AnyRef with SynchronizedObject[AnyRef], owner: String): Unit = {
        if (!isRegistered(treeID)) {
            createNewTree(treeID, owner, new InstanceWrapper[A](rootObject.asInstanceOf[A with SynchronizedObject[A]]), defaultTreeViewBehavior)
        }
    }

    override def isRegistered(id: Int): Boolean = {
        treeCenter.findTree(id).isDefined
    }

    private object DefaultInstantiator extends SyncInstanceInstantiator {

        override def newWrapper[B <: AnyRef](creator: SyncInstanceCreator[B]): B with SynchronizedObject[B] = {
            val syncClass = generator.getSyncClass[B](creator.tpeClass.asInstanceOf[Class[B]])
            creator.getInstance(syncClass)
        }

    }

    private object CenterHandler extends CacheHandler with ContentHandler[CacheRepoContent[A]] with AttachHandler {

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket                        =>
                    handleInvocationPacket(ip, bundle)
                case ObjectPacket(location: SyncObjectReference) =>
                    bundle.responseSubmitter
                        .addPacket(BooleanPacket(isObjectPresent(location)))
                        .submit()

                case ObjectPacket(ObjectTreeProfile(treeID, rootObject: AnyRef with SynchronizedObject[AnyRef], owner)) =>
                    handleRootObjectPacket(treeID, rootObject, owner)
            }
        }

        override def initializeContent(content: CacheRepoContent[A]): Unit = {
            if (content.array.isEmpty)
                return
            val array = content.array

            array.foreach(profile => {
                val rootObject = profile.rootObject
                val owner      = profile.treeOwner
                val treeID     = profile.treeID
                val path       = Array(treeID)
                //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
                if (isRegistered(treeID)) {
                    findNode(path).fold {
                        throw new NoSuchSyncNodeException(s"Unknown local object of path '${path.mkString("/")}'")
                    }((n: SyncNode[A]) => n.chip.updateObject(rootObject))
                }
                //it's an object that must be remotely controlled because it is chipped by another objects cache.
                else {
                    createNewTree(treeID, owner, new InstanceWrapper[A](rootObject), defaultTreeViewBehavior)
                }
            })
        }

        override def getContent: CacheRepoContent[A] = treeCenter.snapshotContent

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

        override val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH] = Some(treeCenter)
    }

}

object DefaultSynchronizedObjectCache {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    implicit def default[A <: AnyRef : ClassTag]: SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = apply()

    def apply[A <: AnyRef : ClassTag](): SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = {
        val treeView = new DefaultObjectBehaviorStore(AnnotationBasedMemberBehaviorFactory)
        apply[A](treeView)
    }

    def apply[A <: AnyRef : ClassTag](store: ObjectBehaviorStore): SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = {
        channel => {
            apply[A](channel, store, channel.traffic.connection.network)
        }
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](network: Network): SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = {
        channel => {
            val treeView = new DefaultObjectBehaviorStore(AnnotationBasedMemberBehaviorFactory)
            apply[A](channel, treeView, network)
        }
    }

    private[linkit] def apply[A <: AnyRef : ClassTag](store: ObjectBehaviorStore, network: Network): SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = {
        channel => {
            apply[A](channel, store, network)
        }
    }

    private def apply[A <: AnyRef : ClassTag](channel: CachePacketChannel, behaviors: ObjectBehaviorStore, network: Network): SynchronizedObjectCache[A] = {
        import fr.linkit.engine.application.resource.external.LocalResourceFolder._
        val context   = channel.manager.network.connection.getApp
        val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val generator = new DefaultSyncClassCenter(context.compilerCenter, resources)

        new DefaultSynchronizedObjectCache[A](channel, generator, behaviors, network)
    }

    case class ObjectTreeProfile[A <: AnyRef](treeID: Int, rootObject: A with SynchronizedObject[A], treeOwner: String) extends Serializable

}