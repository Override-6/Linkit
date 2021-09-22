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
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.generation.{ObjectWrapperInstantiator, SyncClassCenter}
import fr.linkit.api.connection.cache.obj.instantiation.SyncInstanceGetter
import fr.linkit.api.connection.cache.obj.tree.{NoSuchSyncNodeException, SyncNode}
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
import fr.linkit.engine.connection.cache.obj.generation.{DefaultSyncClassCenter, SyncObjectClassResource, SyncObjectInstantiationHelper}
import fr.linkit.engine.connection.cache.obj.instantiation.InstanceWrapper
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.connection.cache.obj.tree._
import fr.linkit.engine.connection.cache.obj.tree.node.{RootWrapperNode, TrafficInterestedSyncNode}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.local.LinkitApplication

import scala.reflect.ClassTag

final class DefaultSynchronizedObjectCenter[A <: AnyRef] private(channel: CachePacketChannel,
                                                                 generator: SyncClassCenter,
                                                                 override val defaultTreeViewBehavior: ObjectBehaviorStore,
                                                                 override val network: Network)
    extends AbstractSharedCache(channel) with SynchronizedObjectCache[A] {

    private  val currentIdentifier                      = channel.traffic.connection.currentIdentifier
    override val treeCenter: DefaultObjectTreeCenter[A] = new DefaultObjectTreeCenter[A](this)
    channel.setHandler(CenterHandler)

    override def syncObject(id: Int, obj: A): A with SynchronizedObject[A] = {
        syncObject(id, obj, defaultTreeViewBehavior)
    }

    override def syncObjectAndReplaceType(id: Int, obj: A): Unit = {
        throw new UnsupportedOperationException("Object replacement is not available currently")
    }

    override def syncObjectAndReplaceType(id: Int, obj: A, behavior: ObjectBehaviorStore): Unit = {
        throw new UnsupportedOperationException("Object replacement is not available currently")
    }

    override def syncObject(id: Int, creator: SyncInstanceGetter[A]): A with SynchronizedObject[A] = {
        syncObject(id, creator, defaultTreeViewBehavior)
    }

    override def syncObject(id: Int, creator: SyncInstanceGetter[A], store: ObjectBehaviorStore): A with SynchronizedObject[A] = {
        val tree = createNewTree(id, currentIdentifier, creator, Map(), store)
        channel.makeRequest(ChannelScopes.discardCurrent)
            .addPacket(ObjectPacket(ObjectTreeProfile(id, tree.getRoot.synchronizedObject, currentIdentifier, Map())))
            .putAllAttributes(this)
            .submit()
            .detach()
        //Indicate that a new object has been posted.
        val wrapperNode = tree.getRoot
        wrapperNode.setPresentOnNetwork()
        wrapperNode.synchronizedObject
    }

    override def syncObject(id: Int, obj: A, behavior: ObjectBehaviorStore): A with SynchronizedObject[A] = {
        ensureNotWrapped(obj)
        if (isRegistered(id))
            throw new ObjectAlreadyPostedException(s"Another object with id '$id' figures in the repo's list.")

        val creator = new SyncInstanceGetter[A] {
            override val tpeClass: Class[_] = obj.getClass

            override def getInstance(syncClass: Class[A with SynchronizedObject[A]]): A with SynchronizedObject[A] = {
                val helper = new SyncObjectInstantiationHelper(DefaultInstantiator, behavior)
                //val clone = SyncObjectInstantiationHelper.deepClone(obj, network.refStore)
                helper.instantiateFromOrigin(syncClass, obj, Map.empty)._1
            }
        }
        syncObject(id, creator, behavior)
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        treeCenter.findTree(id).map(_.rootNode.synchronizedObject)
    }

    override def isRegistered(id: Int): Boolean = {
        treeCenter.findTree(id).isDefined
    }

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[SynchronizedObject[_]])
            throw new CanNotSynchronizeException("This object is already wrapped.")
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

    private def createNewTree(id: Int, rootObjectOwner: String, creator: SyncInstanceGetter[A], subWrappersInfo: Map[AnyRef, SyncNodeInfo], behaviorTree: ObjectBehaviorStore = defaultTreeViewBehavior): DefaultSynchronizedObjectTree[A] = {
        val puppeteerInfo       = SyncNodeInfo(family, cacheID, rootObjectOwner, Array(id))
        val rootBehavior        = behaviorTree.getFromClass[A](creator.tpeClass)
        val (root, subWrappers) = DefaultInstantiator.newWrapper[A](creator, behaviorTree, puppeteerInfo, subWrappersInfo)
        val chip                = ObjectChip[A](rootBehavior, network, root)
        val rootNode            = new RootWrapperNode[A](root.getPuppeteer, chip, _, currentIdentifier, id)
        val tree                = new DefaultSynchronizedObjectTree[A](currentIdentifier, id, DefaultInstantiator, this, behaviorTree)(rootNode)
        treeCenter.addTree(id, tree)

        subWrappers.toSeq
            .sortBy(pair => pair._2.getNodeInfo.nodePath.length)
            .foreach(pair => {
                val wrapper    = pair._2
                val info       = wrapper.getNodeInfo
                val path       = info.nodePath
                val parentPath = path.dropRight(1)
                val id         = path.last
                tree.registerSynchronizedObject(parentPath, id, wrapper, info.owner)
            })

        tree
    }

    private object DefaultInstantiator extends ObjectWrapperInstantiator {

        override def newWrapper[B <: AnyRef](creator: SyncInstanceGetter[B], store: ObjectBehaviorStore,
                                             nodeInfo: SyncNodeInfo, subWrappersInfo: Map[AnyRef, SyncNodeInfo]): (B with SynchronizedObject[B], Map[AnyRef, SynchronizedObject[AnyRef]]) = {
            val syncClass = generator.getSyncClass[B](creator.tpeClass.asInstanceOf[Class[B]])
            val wrapper   = creator.getInstance(syncClass)
            initializeSyncObject(wrapper, nodeInfo, store)
            (wrapper, Map.empty[AnyRef, SynchronizedObject[AnyRef]])
        }

        override def initializeSyncObject[B <: AnyRef](syncObject: SynchronizedObject[B], nodeInfo: SyncNodeInfo, store: ObjectBehaviorStore): Unit = {
            val behavior  = store.getFromClass[B](syncObject.getSuperClass)
            val puppeteer = new ObjectPuppeteer[B](channel, DefaultSynchronizedObjectCenter.this, nodeInfo, behavior)
            syncObject.initPuppeteer(puppeteer, store)
        }

    }

    private object CenterHandler extends CacheHandler with ContentHandler[CacheRepoContent[A]] with AttachHandler {

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket                                                                                  =>
                    val path     = ip.path
                    val node     = findNode(path)
                    val senderID = bundle.coords.senderID
                    node.fold(AppLogger.error(s"Could not find rootObject node at path ${path.mkString("/")}")) {
                        case node: TrafficInterestedSyncNode[_] => node.handlePacket(ip, senderID, bundle.responseSubmitter)
                        case _                                  =>
                            throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedSyncNode[_]].getSimpleName} in order to handle a member rmi request.")
                    }
                case ObjectPacket(ObjectTreeProfile(treeID, rootObject: Any with SynchronizedObject[Any], owner, subWrappers)) =>
                    if (!isRegistered(treeID)) {
                        val tree = createNewTree(treeID, owner, new InstanceWrapper[A](rootObject.asInstanceOf[A with SynchronizedObject[A]]), subWrappers, defaultTreeViewBehavior)
                        tree.getRoot.setPresentOnNetwork()
                    }
            }
        }

        override def setContent(content: CacheRepoContent[A]): Unit = {
            if (content.array.isEmpty)
                return
            val array = content.array

            array.foreach(profile => {
                val rootObject  = profile.rootObject
                val owner       = profile.treeOwner
                val treeID      = profile.treeID
                val subWrappers = profile.subWrappers
                val path        = Array(treeID)
                //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
                if (isRegistered(treeID)) {
                    findNode(path).fold {
                        throw new NoSuchSyncNodeException(s"Unknown local object of path '${path.mkString("/")}'")
                    }((n: SyncNode[A]) => n.chip.updateObject(rootObject))
                }
                //it's an object that must be remotely controlled because it is chipped by another objects cache.
                else {
                    val tree = createNewTree(treeID, owner, new InstanceWrapper[A](rootObject), subWrappers, defaultTreeViewBehavior)
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

    def apply[A <: AnyRef : ClassTag](store: ObjectBehaviorStore): SharedCacheFactory[SynchronizedObjectCache[A] with SharedCache] = {
        channel => {
            apply[A](channel, store, channel.traffic.connection.network)
        }
    }

    private def apply[A <: AnyRef : ClassTag](channel: CachePacketChannel, behaviors: ObjectBehaviorStore, network: Network): SynchronizedObjectCache[A] = {
        import fr.linkit.engine.local.resource.external.LocalResourceFolder._
        val context   = channel.manager.network.connection.getApp
        val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
        val generator = new DefaultSyncClassCenter(context.compilerCenter, resources)

        new DefaultSynchronizedObjectCenter[A](channel, generator, behaviors, network)
    }

    case class ObjectTreeProfile[A](treeID: Int, rootObject: A with SynchronizedObject[A], treeOwner: String, subWrappers: Map[AnyRef, SyncNodeInfo]) extends Serializable

}