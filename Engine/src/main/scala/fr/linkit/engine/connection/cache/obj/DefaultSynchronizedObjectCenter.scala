/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj

import fr.linkit.api.connection.cache.obj._
import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.generation.SyncClassCenter
import fr.linkit.api.connection.cache.obj.instantiation.{SyncInstanceInstantiator, SyncInstanceGetter}
import fr.linkit.api.connection.cache.obj.tree.{NoSuchSyncNodeException, SyncNode, SyncNodeLocation}
import fr.linkit.api.connection.cache.traffic.CachePacketChannel
import fr.linkit.api.connection.cache.traffic.handler.{AttachHandler, CacheHandler, ContentHandler}
import fr.linkit.api.connection.cache.{SharedCache, SharedCacheFactory}
import fr.linkit.api.connection.network.{Engine, Network}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.RequestPacketBundle
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.AbstractSharedCache
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter.ObjectTreeProfile
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, DefaultObjectBehaviorStore}
import fr.linkit.engine.connection.cache.obj.generation.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.connection.cache.obj.instantiation.InstanceWrapper
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.connection.cache.obj.tree._
import fr.linkit.engine.connection.cache.obj.tree.node._
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.local.LinkitApplication

import scala.reflect.ClassTag

final class DefaultSynchronizedObjectCenter[A <: AnyRef] private(channel: CachePacketChannel,
                                                                 generator: SyncClassCenter,
                                                                 override val defaultTreeViewBehavior: ObjectBehaviorStore,
                                                                 override val network: Network)
        extends AbstractSharedCache(channel) with SynchronizedObjectCache[A] with SyncNodeDataFactory {

    override val treeCenter: DefaultObjectTreeCenter[A] = new DefaultObjectTreeCenter[A](this)
    private  val currentIdentifier                      = channel.traffic.connection.currentIdentifier
    channel.setHandler(CenterHandler)

    override def syncObject(id: Int, creator: SyncInstanceGetter[A]): A with SynchronizedObject[A] = {
        syncObject(id, creator, defaultTreeViewBehavior)
    }

    override def syncObject(id: Int, creator: SyncInstanceGetter[A], store: ObjectBehaviorStore): A with SynchronizedObject[A] = {
        val tree = createNewTree(id, currentIdentifier, creator, store)
        channel.makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket(ObjectTreeProfile(id, tree.getRoot.synchronizedObject, currentIdentifier)))
                .putAllAttributes(this)
                .submit()
                .detach()
        //Indicate that a new object has been posted.
        val wrapperNode = tree.getRoot
        wrapperNode.setPresentOnNetwork()
        wrapperNode.synchronizedObject
    }

    private def createNewTree(id: Int, rootObjectOwner: String, creator: SyncInstanceGetter[A], behaviorTree: ObjectBehaviorStore = defaultTreeViewBehavior): DefaultSynchronizedObjectTree[A] = {
        val nodeLocation = SyncNodeLocation(family, cacheID, rootObjectOwner, Array(id))
        val rootBehavior = behaviorTree.getFromClass[A](creator.tpeClass)
        val root         = DefaultInstantiator.newWrapper[A](creator)
        val chip         = ObjectChip[A](rootBehavior, network, root)
        val puppeteer    = new ObjectPuppeteer[A](channel, this, nodeLocation, rootBehavior)
        val rootNode     = (tree: DefaultSynchronizedObjectTree[A]) => {
            val data = new ObjectNodeData[A](puppeteer, chip, tree, nodeLocation, root, currentIdentifier)
            new RootObjectSyncNode[A](data)
        }
        val tree         = new DefaultSynchronizedObjectTree[A](currentIdentifier, network, id, DefaultInstantiator, this, behaviorTree)(rootNode)
        treeCenter.addTree(id, tree)
        /*
                subWrappers.toSeq
                        .sortBy(pair => pair._2.location.nodePath.length)
                        .foreach(pair => {
                            val wrapper    = pair._2
                            val info       = wrapper.location
                            val path       = info.nodePath
                            val parentPath = path.dropRight(1)
                            val id         = path.last
                            tree.registerSynchronizedObject(parentPath, id, wrapper, info.owner)
                        })*/

        tree
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
        val nodeLocation  = SyncNodeLocation(family, cacheID, ownerID, path)
        val puppeteer     = new ObjectPuppeteer[B](channel, this, nodeLocation, behavior)
        new ObjectNodeData[B](puppeteer, chip, tree, nodeLocation, syncObject, currentIdentifier)
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        treeCenter.findTree(id).map(_.rootNode.synchronizedObject)
    }

    def isObjectPresent(location: SyncNodeLocation): Boolean = {
        (location.cacheID == cacheID) && location.cacheFamily == family && {
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

    private def handleRootObjectPacket(treeID: Int, rootObject: Any with SynchronizedObject[Any], owner: String): Unit = {
        if (!isRegistered(treeID)) {
            val tree = createNewTree(treeID, owner, new InstanceWrapper[A](rootObject.asInstanceOf[A with SynchronizedObject[A]]), defaultTreeViewBehavior)
            tree.getRoot.setPresentOnNetwork()
        }
    }

    override def isRegistered(id: Int): Boolean = {
        treeCenter.findTree(id).isDefined
    }

    private object DefaultInstantiator extends SyncInstanceInstantiator {

        override def newWrapper[B <: AnyRef](creator: SyncInstanceGetter[B]): B with SynchronizedObject[B] = {
            val syncClass = generator.getSyncClass[B](creator.tpeClass.asInstanceOf[Class[B]])
            creator.getInstance(syncClass)
        }

    }

    private object CenterHandler extends CacheHandler with ContentHandler[CacheRepoContent[A]] with AttachHandler {

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket                     =>
                    handleInvocationPacket(ip, bundle)
                case ObjectPacket(location: SyncNodeLocation) =>
                    bundle.responseSubmitter
                            .addPacket(BooleanPacket(isObjectPresent(location)))
                            .submit()

                case ObjectPacket(ObjectTreeProfile(treeID, rootObject: Any with SynchronizedObject[Any], owner)) =>
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
                    val tree = createNewTree(treeID, owner, new InstanceWrapper[A](rootObject), defaultTreeViewBehavior)
                    tree.getRoot.setPresentOnNetwork()
                }
            })
        }

        override def getContent: CacheRepoContent[A] = treeCenter.snapshotContent

        override def onEngineAttached(engine: Engine): Unit = {
            AppLogger.debug(s"Engine ${engine.identifier} attached to this synchronized object cache !")
        }

        override def inspectEngine(engine: Engine, requestedCacheType: Class[_]): Option[String] = {
            val clazz = classOf[DefaultSynchronizedObjectCenter[A]]
            if (requestedCacheType eq clazz)
                None
            else Some(s"Requested cache class is not ${clazz.getName} (received: ${requestedCacheType.getName}).")
        }

        override def onEngineDetached(engine: Engine): Unit = {
            AppLogger.debug(s"Engine ${engine.identifier} detached to this synchronized object cache !")
        }

    }

}

object DefaultSynchronizedObjectCenter {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

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
        import fr.linkit.engine.local.resource.external.LocalResourceFolder._
        val context   = channel.manager.network.connection.getApp
        val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val generator = new DefaultSyncClassCenter(context.compilerCenter, resources)

        new DefaultSynchronizedObjectCenter[A](channel, generator, behaviors, network)
    }

    case class ObjectTreeProfile[A](treeID: Int, rootObject: A with SynchronizedObject[A], treeOwner: String) extends Serializable

}