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
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, PuppeteerInfo}
import fr.linkit.api.connection.cache.obj.generation.{ObjectWrapperClassGenerator, ObjectWrapperInstantiator}
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.{CacheContent, InternalSharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.concurrency.Procrastinator
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.AbstractSharedCache
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter.ObjectTreeProfile
import fr.linkit.engine.connection.cache.obj.description.ObjectTreeDefaultBehavior
import fr.linkit.engine.connection.cache.obj.description.annotation.AnnotationBasedMemberBehaviorFactory
import fr.linkit.engine.connection.cache.obj.generation.{CloneHelper, ObjectWrapperClassClassGenerator, WrappersClassResource}
import fr.linkit.engine.connection.cache.obj.invokation.local.{ObjectChip, SimpleRMIHandler}
import fr.linkit.engine.connection.cache.obj.invokation.remote.{InstancePuppeteer, InvocationPacket}
import fr.linkit.engine.connection.cache.obj.tree._
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.resource.external.LocalResourceFolder._

import scala.reflect.ClassTag

//TODO Redesign the NetworkSharedCacheManager and its way to handle caches
class DefaultSynchronizedObjectCenter[A](handler: SharedCacheManager,
                                         cacheID: Int,
                                         channel: RequestPacketChannel,
                                         generator: ObjectWrapperClassGenerator,
                                         override val defaultTreeViewBehavior: ObjectTreeBehavior)
    extends AbstractSharedCache(handler, cacheID, channel) with SynchronizedObjectCenter[A] {

    private val procrastinator: Procrastinator = handler.network.connection

    override val treeCenter        = new DefaultObjectTreeCenter[A](this)
    private  val currentIdentifier = channel.traffic.currentIdentifier

    override def postObject(id: Int, obj: A): A with PuppetWrapper[A] = {
        postObject(id, obj, defaultTreeViewBehavior)
    }

    override def postObject(id: Int, obj: A, behavior: ObjectTreeBehavior): A with PuppetWrapper[A] = {
        ensureNotWrapped(obj)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        val tree = createNewTree(id, currentIdentifier, obj, behavior)
        //Indicate that a new object has been posted.
        channel.makeRequest(ChannelScopes.discardCurrent)
            .addPacket(ObjectPacket(ObjectTreeProfile(id, obj, currentIdentifier)))
            .putAllAttributes(this)
            .submit()
        val wrapperNode = tree.getRoot
        wrapperNode.setPresentOnNetwork()
        wrapperNode.synchronizedObject
    }

    override def findObject(id: Int): Option[A with PuppetWrapper[A]] = {
        treeCenter.findTree(id).map(_.rootNode.synchronizedObject)
    }

    override def isRegistered(id: Int): Boolean = {
        treeCenter.findTree(id).isDefined
    }


    private def isRegistered(path: Array[Int]): Boolean = {
        treeCenter.findTree(path.head).exists(_.findNode(path.drop(1)).isDefined)
    }

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[PuppetWrapper[_]])
            throw new IllegalObjectWrapperException("This object is already wrapped.")
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle : ${bundle}")
        val response = bundle.packet
        val owner    = bundle.coords.senderID
        response.nextPacket[Packet] match {
            case ip: InvocationPacket                                          =>
                val path = ip.path
                val node = findNode(path)
                node.fold(AppLogger.error(s"Could not find puppet node at path ${path.mkString("$", " -> ", "")}")) {
                    case node: TrafficInterestedSyncNode[_] => node.handlePacket(ip, bundle.responseSubmitter)
                    case _                                  =>
                        throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedSyncNode[_]].getSimpleName} in order to handle a member rmi request.")
                }
            case ObjectPacket(ObjectTreeProfile(treeID, rootObject: A, owner)) =>
                if (!isRegistered(treeID)) {
                    createNewTree(treeID, owner, rootObject, defaultTreeViewBehavior)
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


    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        if (content.array.isEmpty)
            return
        val array = content.array

        array.foreach(profile => {
            val puppet = profile.puppet
            val owner  = profile.owner
            val treeID = profile.treeID
            val path   = Array(treeID)
            //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
            if (owner == currentIdentifier) {
                findNode(path).fold {
                    throw new NoSuchWrapperNodeException(s"Unknown local object of path '${path.mkString("$", " -> ", "")}'")
                }((n: SyncNode[A]) => n.chip.updateObject(puppet))
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!isRegistered(treeID)) {
                createNewTree(treeID, owner, puppet, defaultTreeViewBehavior)
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
                    tree.findNode[A](path.drop(1))
            })
    }
/*
    private def registerWrapper[B](path: Array[Int], ownerID: String, puppet: B, behaviorTree: ObjectTreeBehavior): B with PuppetWrapper[B] = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty.")
        val treeID                          = path.head
        val tree: SynchronizedObjectTree[A] = treeCenter.findTree(treeID).getOrElse {
            throw new NoSuchWrapperNodeException(s"Could not find object tree of id $treeID.")
        }
        tree.insertObject(path.dropRight(1), path.last, puppet, ownerID).synchronizedObject
    }*/

    private def createNewTree(id: Int, rootObjectOwner: String, rootObject: A, behaviorTree: ObjectTreeBehavior): DefaultSynchronizedObjectTree[A] = {
        val puppeteerInfo = PuppeteerInfo(family, cacheID, rootObjectOwner, Array(id))
        val rootBehavior  = behaviorTree.getFromClass[A](rootObject.getClass)
        val wrapper       = WrapperInstantiator.newWrapper[A](rootObject, behaviorTree, puppeteerInfo)
        val chip          = ObjectChip[A](rootObjectOwner, rootBehavior, wrapper)
        val rootNode      = new RootWrapperNode[A](wrapper.getPuppeteer, chip, _, currentIdentifier, id)
        val tree          = new DefaultSynchronizedObjectTree[A](currentIdentifier, id, WrapperInstantiator, this, behaviorTree)(rootNode)
        treeCenter.addTree(id, tree)
        tree
    }

    private object WrapperInstantiator extends ObjectWrapperInstantiator {
        override def newWrapper[B](obj: B, behaviorTree: ObjectTreeBehavior, puppeteerInfo: PuppeteerInfo): B with PuppetWrapper[B] = {
            val behavior     = behaviorTree.getFromClass[B](obj.getClass)
            val puppeteer    = new InstancePuppeteer[B](channel, procrastinator, DefaultSynchronizedObjectCenter.this, puppeteerInfo, behavior)
            val wrapperClass = generator.getWrapperClass[B](obj.getClass.asInstanceOf[Class[B]])
            val instance     = CloneHelper.instantiateFromOrigin[B](wrapperClass, obj)
            instance.initPuppeteer(puppeteer)
            instance
        }
    }

}

object DefaultSynchronizedObjectCenter {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    def apply[A: ClassTag](): SharedCacheFactory[SynchronizedObjectCenter[A] with InternalSharedCache] = {
        val treeView = new ObjectTreeDefaultBehavior(AnnotationBasedMemberBehaviorFactory(SimpleRMIHandler))
        apply[A](treeView)
    }

    def apply[A: ClassTag](behaviors: ObjectTreeBehavior): SharedCacheFactory[SynchronizedObjectCenter[A] with InternalSharedCache] = {
        (handler: SharedCacheManager, identifier: Int, container: PacketInjectableContainer) => {
            val channel   = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            val context   = handler.network.connection.getApp
            val resources = context.getAppResources.getOrOpenThenRepresent[WrappersClassResource](ClassesResourceDirectory)
            val generator = new ObjectWrapperClassClassGenerator(context.compilerCenter, resources)

            new DefaultSynchronizedObjectCenter[A](handler, identifier, channel, generator, behaviors)
        }
    }

    case class ObjectTreeProfile[A](treeID: Int, puppet: A, owner: String) extends Serializable

}