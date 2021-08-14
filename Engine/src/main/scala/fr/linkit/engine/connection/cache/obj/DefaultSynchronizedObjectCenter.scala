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
import fr.linkit.api.connection.cache.obj.behavior.{ObjectTreeBehavior, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.generation.{ObjectWrapperClassCenter, ObjectWrapperInstantiator}
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode}
import fr.linkit.api.connection.cache.traffic.CachePacketChannel
import fr.linkit.api.connection.cache.{CacheContent, SharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.AbstractSharedCache
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter.ObjectTreeProfile
import fr.linkit.engine.connection.cache.obj.behavior.{AnnotationBasedMemberBehaviorFactory, ObjectTreeDefaultBehavior}
import fr.linkit.engine.connection.cache.obj.generation.{DefaultObjectWrapperClassCenter, SyncObjectClassResource, SyncObjectInstantiationHelper}
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.invokation.remote.{InstancePuppeteer, InvocationPacket}
import fr.linkit.engine.connection.cache.obj.tree._
import fr.linkit.engine.connection.cache.obj.tree.node.{RootWrapperNode, TrafficInterestedSyncNode}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestPacketBundle, RequestPacketChannel}
import fr.linkit.engine.local.LinkitApplication

import scala.reflect.ClassTag

//TODO Redesign the NetworkSharedCacheManager and its way to handle caches
final class DefaultSynchronizedObjectCenter[A <: AnyRef] private(channel: CachePacketChannel,
                                                                 generator: ObjectWrapperClassCenter,
                                                                 override val defaultTreeViewBehavior: ObjectTreeBehavior)
        extends AbstractSharedCache(handler, cacheID, channel) with SynchronizedObjectCenter[A] {

    private val procrastinator: ProcrastinatorControl = handler.network.connection

    override val treeCenter: DefaultObjectTreeCenter[A] = new DefaultObjectTreeCenter[A](this)
    private  val currentIdentifier                      = channel.traffic.currentIdentifier

    override def postObject(id: Int, obj: A): A with SynchronizedObject[A] = {
        postObject(id, obj, defaultTreeViewBehavior)
    }

    override def postObject(id: Int, obj: A, behavior: ObjectTreeBehavior): A with SynchronizedObject[A] = {
        ensureNotWrapped(obj)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        val objClone = SyncObjectInstantiationHelper.deepClone(obj)
        val tree     = createNewTree(id, currentIdentifier, objClone, Map(), behavior)
        //Indicate that a new object has been posted.
        channel.makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket(ObjectTreeProfile(id, obj, currentIdentifier, Map())))
                .putAllAttributes(this)
                .submit()
        val wrapperNode = tree.getRoot
        wrapperNode.setPresentOnNetwork()
        wrapperNode.synchronizedObject
    }

    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        treeCenter.findTree(id).map(_.rootNode.synchronizedObject)
    }

    override def isRegistered(id: Int): Boolean = {
        treeCenter.findTree(id).isDefined
    }

    override protected def handleBundle(bundle: RequestPacketBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle : ${bundle}")
        val response = bundle.packet
        response.nextPacket[Packet] match {
            case ip: InvocationPacket                                                       =>
                val path = ip.path
                val node = findNode(path)
                node.fold(AppLogger.error(s"Could not find puppet node at path ${path.mkString("/")}")) {
                    case node: TrafficInterestedSyncNode[_] => node.handlePacket(ip, bundle.responseSubmitter)
                    case _                                  =>
                        throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedSyncNode[_]].getSimpleName} in order to handle a member rmi request.")
                }
            case ObjectPacket(ObjectTreeProfile(treeID, rootObject: A, owner, subWrappers)) =>
                if (!isRegistered(treeID)) {
                    val tree = createNewTree(treeID, owner, rootObject, subWrappers, defaultTreeViewBehavior)
                    tree.getRoot.setPresentOnNetwork()
                }
        }
    }

    override def setContent(content: CacheContent): Unit = {
        content match {
            case content: CacheRepoContent[A] => setRepoContent(content)
            case _                            => throw new IllegalArgumentException(s"Received unknown content '$content'")
        }
    }

    override def snapshotContent: CacheRepoContent[A] = treeCenter.snapshotContent

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[SynchronizedObject[_]])
            throw new IllegalObjectWrapperException("This object is already wrapped.")
    }

    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        if (content.array.isEmpty)
            return
        val array = content.array

        array.foreach(profile => {
            val puppet      = profile.rootObject
            val owner       = profile.treeOwner
            val treeID      = profile.treeID
            val subWrappers = profile.subWrappers
            val path        = Array(treeID)
            //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
            if (isRegistered(treeID)) {
                findNode(path).fold {
                    throw new NoSuchWrapperNodeException(s"Unknown local object of path '${path.mkString("/")}'")
                }((n: SyncNode[A]) => n.chip.updateObject(puppet))
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else {
                val tree = createNewTree(treeID, owner, puppet, subWrappers, defaultTreeViewBehavior)
                tree.getRoot.setPresentOnNetwork()
            }
        })
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

    private def createNewTree(id: Int, rootObjectOwner: String, rootObject: A, subWrappersInfo: Map[AnyRef, WrapperNodeInfo], behaviorTree: ObjectTreeBehavior = defaultTreeViewBehavior): DefaultSynchronizedObjectTree[A] = {
        val puppeteerInfo       = WrapperNodeInfo(family, cacheID, rootObjectOwner, Array(id))
        val rootBehavior        = behaviorTree.getFromClass[A](rootObject.getClass)
        val (root, subWrappers) = WrapperInstantiator.newWrapper[A](rootObject, behaviorTree, puppeteerInfo, subWrappersInfo)
        val chip                = ObjectChip[A](rootBehavior, root)
        val rootNode            = new RootWrapperNode[A](root.getPuppeteer, chip, _, currentIdentifier, id)
        val tree                = new DefaultSynchronizedObjectTree[A](currentIdentifier, id, WrapperInstantiator, this, behaviorTree)(rootNode)
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

    private object WrapperInstantiator extends ObjectWrapperInstantiator {

        override def newWrapper[B <: AnyRef](obj: B, behaviorTree: ObjectTreeBehavior,
                                             nodeInfo: WrapperNodeInfo, subWrappersInfo: Map[AnyRef, WrapperNodeInfo]): (B with SynchronizedObject[B], Map[AnyRef, SynchronizedObject[AnyRef]]) = {
            val wrapperClass           = generator.getWrapperClass[B](obj.getClass.asInstanceOf[Class[B]])
            val helper                 = new SyncObjectInstantiationHelper(this, behaviorTree)
            val (wrapper, subWrappers) = helper.instantiateFromOrigin[B](wrapperClass, obj, subWrappersInfo)
            val behavior               = behaviorTree.getFromClass[B](obj.getClass)
            initializeWrapper(wrapper, nodeInfo, behavior)
            (wrapper, subWrappers)
        }

        override def initializeWrapper[B <: AnyRef](wrapper: SynchronizedObject[B], nodeInfo: WrapperNodeInfo, behavior: WrapperBehavior[B]): Unit = {
            val puppeteer = new InstancePuppeteer[B](channel, procrastinator, DefaultSynchronizedObjectCenter.this, nodeInfo, behavior)
            wrapper.initPuppeteer(puppeteer)
        }

    }

}

object DefaultSynchronizedObjectCenter {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    def apply[A <: AnyRef : ClassTag](): SharedCacheFactory[SynchronizedObjectCenter[A] with SharedCache] = {
        val treeView = new ObjectTreeDefaultBehavior(AnnotationBasedMemberBehaviorFactory)
        apply[A](treeView)
    }

    def apply[A <: AnyRef : ClassTag](behaviors: ObjectTreeBehavior): SharedCacheFactory[SynchronizedObjectCenter[A] with SharedCache] = {
        channel => {
            val context = channel.manager.network.connection.getApp
            import fr.linkit.engine.local.resource.external.LocalResourceFolder._
            val resources = context.getAppResources.getOrOpenThenRepresent[SyncObjectClassResource](ClassesResourceDirectory)
            val generator = new DefaultObjectWrapperClassCenter(context.compilerCenter, resources)

            new DefaultSynchronizedObjectCenter[A](handler, identifier, channel, generator, behaviors)
        }
    }

    case class ObjectTreeProfile[A](treeID: Int, rootObject: A, treeOwner: String, subWrappers: Map[AnyRef, WrapperNodeInfo]) extends Serializable

}