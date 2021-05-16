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

package fr.linkit.engine.connection.cache.repo

import fr.linkit.api.connection.cache.repo._
import fr.linkit.api.connection.cache.repo.generation.{PuppetWrapperGenerator, PuppeteerDescription}
import fr.linkit.api.connection.cache.{CacheContent, InternalSharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.AbstractSharedCache
import fr.linkit.engine.connection.cache.repo.CloudObjectRepository.{FieldRestorer, PuppetProfile}
import fr.linkit.engine.connection.cache.repo.generation.{PuppetWrapperClassGenerator, WrappersClassResource}
import fr.linkit.engine.connection.cache.repo.tree.{DefaultPuppetCenter, PuppetNode}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.Field
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.reflect.ClassTag

class CloudObjectRepository[A <: Serializable](handler: SharedCacheManager,
                                               cacheID: Int,
                                               channel: RequestPacketChannel,
                                               generator: PuppetWrapperGenerator,
                                               override val descriptions: PuppetDescriptions)
        extends AbstractSharedCache(handler, cacheID, channel) with ObjectRepository[A] {

    override val center            = new DefaultPuppetCenter[A]
    private  val fieldRestorer     = new FieldRestorer
    private  val supportIdentifier = channel.traffic.supportIdentifier

    override def getPuppetDescription[B <: A : ClassTag]: PuppetDescription[B] = descriptions.getDescription[B]

    override def postObject[B <: A : ClassTag](id: Int, puppet: B): B with PuppetWrapper[B] = {
        ensureNotWrapped(puppet)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        registerRemotePuppet[B](Array(id), supportIdentifier, puppet)
    }

    override def findObject[B <: A](id: Int): Option[B with PuppetWrapper[B]] = {
        center.getPuppet[B](Array(id)).map(_.puppeteer.getPuppetWrapper)
    }

    override def isRegistered(id: Int): Boolean = {
        isRegistered(Array(id))
    }

    override def initPuppetWrapper[B <: A : ClassTag](wrapper: B with PuppetWrapper[B]): Unit = {
        if (wrapper.isInitialized)
            return
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, getPuppetDescription[B])
        wrapper.initPuppeteer(puppeteer, wrapper)
    }

    private def isRegistered(path: Array[Int]): Boolean = {
        center.getPuppet(path).isDefined
    }

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[PuppetWrapper[_]])
            throw new IllegalPuppetException("This object is already shared.")
    }

    private def genPuppetWrapper[B: ClassTag](puppeteer: Puppeteer[B], puppet: B): B with PuppetWrapper[B] = {
        val puppetClass = generator.getClass[B](puppet.getClass.asInstanceOf[Class[B]])
        instantiatePuppetWrapper[B](puppeteer, puppet, puppetClass)
    }

    private def instantiatePuppetWrapper[B](puppeteer: Puppeteer[B], clone: B, puppetClass: Class[B with PuppetWrapper[B]]): B with PuppetWrapper[B] = {
        val constructor = puppetClass.getDeclaredConstructor(classOf[Puppeteer[_]], clone.getClass)
        constructor.newInstance(puppeteer, clone)
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle : ${bundle}")
        val response = bundle.packet
        val owner    = bundle.coords.senderID
        response.nextPacket[Packet] match {
            case ObjectPacket(profile: PuppetProfile[A]) =>
                if (!isRegistered(profile.treeViewPath)) {
                    val puppet = profile.puppet
                    fieldRestorer.restoreFields(puppet)
                    registerRemotePuppet(profile.treeViewPath, owner, puppet)(ClassTag(puppet.getClass))
                }
            case _ =>
        }
    }

    override def setContent(content: CacheContent): Unit = {
        content match {
            case content: CacheRepoContent[A] => setRepoContent(content)
            case _                            => throw new IllegalArgumentException(s"Received unknown content '$content'")
        }
    }

    override def snapshotContent: CacheContent = center.snapshotContent

    override def genSynchronizedObject[B: ClassTag](treeViewPath: Array[Int],
                                                    obj: B,
                                                    owner: String,
                                                    descriptions: PuppetDescriptions)(foreachSyncObjAction: (PuppetWrapper[_ <: Any], Array[Int]) => Unit): B with PuppetWrapper[B] = {
        ensureNotWrapped(obj)

        if (treeViewPath.isEmpty)
            throw new IllegalArgumentException("Synchronized object's tree view path must not be empty.")

        if (obj == null)
            throw new NullPointerException

        val puppetDesc    = descriptions.getDescription[B]
        val puppeteerDesc = PuppeteerDescription(family, cacheID, owner, treeViewPath)
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, puppetDesc)
        val wrapper       = genPuppetWrapper[B](puppeteer, obj)
        foreachSyncObjAction(wrapper, treeViewPath)
        for (desc <- puppetDesc.listFields() if desc.isSynchronized) {
            val id            = ThreadLocalRandom.current().nextInt()
            val field         = desc.field
            val fieldValue    = field.get(wrapper)
            val childTreePath = treeViewPath ++ Array(id)
            val syncObject    = genSynchronizedObject(childTreePath, fieldValue, owner, descriptions)(foreachSyncObjAction)(ClassTag(fieldValue.getClass))
            ScalaUtils.setValue(wrapper, field, syncObject)
        }
        wrapper
    }

    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        if (content.array.isEmpty)
            return
        val array = content.array

        array.foreach(profile => {
            val puppet = profile.puppet
            val owner  = profile.owner
            val path   = profile.treeViewPath
            //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
            if (owner == supportIdentifier) {
                center.getPuppet[A](path).fold {
                    throw new IllegalArgumentException(s"Unknown local object of path '${path.mkString("$", " -> ", "")}'")
                }(_.chip.updateAllFields(puppet))
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!isRegistered(path)) {
                registerRemotePuppet(path, owner, puppet)(ClassTag(puppet.getClass))
            }
        })
    }

    private def registerRemotePuppet[B <: A : ClassTag](path: Array[Int], owner: String, puppet: B): B with PuppetWrapper[B] = {
        val puppeteerDesc = PuppeteerDescription(family, cacheID, owner, path)
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, getPuppetDescription[B])
        val chip          = ObjectChip[B](owner, getPuppetDescription[B], puppet)
        val intended      = owner == supportIdentifier

        center.addPuppet(path, new PuppetNode[B](puppeteer, chip, descriptions, intended, path.last, _))
        val node = center.getPuppet(path).get
        val wrappedPuppet = genSynchronizedObject(path, puppet, owner, descriptions) {
            (wrapper, childPath) =>
                val id          = childPath.last
                val description = descriptions.getDescription[Any](ClassTag(wrapper.getClass))
                node.getGrandChild(childPath.drop(path.length).dropRight(1))
                        .fold(throw new NoSuchPuppetException(s"Puppet Node not found in path ${childPath.mkString("$", " -> ", "")}")) {
                            parent =>
                                val chip = ObjectChip[Any](owner, description, wrapper)
                                val puppeteer = wrapper.getPuppeteer.asInstanceOf[Puppeteer[Any]]
                                parent.addChild(id, new PuppetNode(puppeteer, chip, descriptions, intended, id, _))
                        }
        }
        channel.makeRequest(ChannelScopes.broadcast)
                .addPacket(ObjectPacket(PuppetProfile(path, puppet, owner)))
                .submit()
        wrappedPuppet
    }
}

object CloudObjectRepository {

    private val ClassesResourceDirectory = "/PuppetGeneration/"

    def apply[A <: Serializable : ClassTag](descriptions: PuppetDescriptions = PuppetDescriptions.getDefault): SharedCacheFactory[CloudObjectRepository[A] with InternalSharedCache] = {
        (handler: SharedCacheManager, identifier: Int, container: PacketInjectableContainer) => {
            val channel     = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            val application = handler.network.connection.getContext
            val resources   = application.getAppResources.getOrOpenShort[WrappersClassResource](ClassesResourceDirectory)
            val generator   = new PuppetWrapperClassGenerator(resources)

            new CloudObjectRepository[A](handler, identifier, channel, generator, descriptions)
        }
    }

    class FieldRestorer {

        private val values = mutable.HashMap.empty[Field, Any]

        def putValue(field: Field, value: Any): Unit = {
            field.setAccessible(true)
            values.put(field, value)
        }

        def restoreFields(any: Any): Unit = {
            val clazz = any.getClass

            listRestorableFields(clazz).foreach(field => {
                ScalaUtils.setValue(any, field, values(field))
            })
        }

        private def listRestorableFields(clazz: Class[_]): Array[Field] = {
            if (clazz == null)
                return Array.empty
            clazz.getDeclaredFields.filter(values.contains) ++
                    listRestorableFields(clazz.getSuperclass) ++
                    clazz.getInterfaces.flatMap(listRestorableFields)
        }
    }

    case class PuppetProfile[A <: Serializable](treeViewPath: Array[Int], puppet: A, owner: String) extends Serializable

}