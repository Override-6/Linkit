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
import fr.linkit.engine.connection.cache.repo.CloudPuppetRepository.{CacheRepoContent, FieldRestorer, PuppetProfile}
import fr.linkit.engine.connection.cache.repo.generation.{PuppetWrapperClassGenerator, WrappersClassResource}
import fr.linkit.engine.connection.cache.{AbstractSharedCache, CacheArrayContent}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.Field
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class CloudPuppetRepository[A <: Serializable](handler: SharedCacheManager,
                                               cacheID: Int,
                                               channel: RequestPacketChannel,
                                               generator: PuppetWrapperGenerator)
        extends AbstractSharedCache(handler, cacheID, channel) with PuppetRepository[A] {

    private val localChips        = new mutable.HashMap[Int, ObjectChip[_ <: A]]()
    private val puppeteers        = new mutable.HashMap[Int, Puppeteer[_ <: A]]()
    private val unFlushedPuppets  = new mutable.HashSet[PuppetProfile[_ <: A]]
    private val descriptions      = new mutable.HashMap[Class[_ <: A], PuppetDescription[_ <: A]]
    private val fieldRestorer     = new FieldRestorer
    private val supportIdentifier = channel.traffic.supportIdentifier

    AppLogger.vTrace(s"Shared Object cache opened (id: $cacheID, family: ${handler.family}, owner: ${handler.ownerID})")

    override def getPuppetDescription[B <: A : ClassTag]: PuppetDescription[B] = {
        val rClass = classTag[B].runtimeClass.asInstanceOf[Class[B]]
        descriptions.getOrElseUpdate(rClass, new PuppetDescription[B](rClass))
                .asInstanceOf[PuppetDescription[B]]
    }

    override def postObject[B <: A : ClassTag](id: Int, obj: B): B with PuppetWrapper[B] = {
        ensureNotWrapped(obj)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        chipObject[B](id, obj)
        flushPuppet(PuppetProfile(id, obj, supportIdentifier))
        genPuppetWrapper[B](id, supportIdentifier, obj)
    }

    override def findObject[B <: A](id: Int): Option[B with PuppetWrapper[B]] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer.getPuppet match {
                case null    => None
                case _: B => Option(puppeteer.getPuppetWrapper.asInstanceOf[B with PuppetWrapper[B]])
            }
        }
    }

    override def isRegistered(identifier: Int): Boolean = {
        puppeteers.contains(identifier) || unFlushedPuppets.exists(_.id == identifier)
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle : ${bundle}")
        val response = bundle.packet
        val id       = response.getAttribute[Int]("id").get
        val owner    = bundle.coords.senderID

        response.nextPacket[Packet] match {
            case ObjectPacket(profile: PuppetProfile[A]) =>
                if (!localChips.contains(id)) {
                    val puppet = profile.puppet
                    fieldRestorer.restoreFields(puppet)
                    genPuppetWrapper[A](id, owner, puppet)(ClassTag(puppet.getClass))
                }

            case reqPacket => localChips.get(id).fold() { chip =>
                chip.handleBundle(reqPacket, bundle.responseSubmitter)
            }
        }
    }

    def getChip[S <: Serializable](id: Int): Option[ObjectChip[S]] = {
        localChips.get(id) match {
            case None       => None
            case Some(chip) => chip match {
                case chip: ObjectChip[S] => Some(chip)
                case _                   => None
            }
        }
    }

    override def setContent(content: CacheContent): Unit = {
        content match {
            case content: CacheRepoContent[A] => setRepoContent(content)
            case _                            => throw new IllegalArgumentException(s"Received unknown content '$content'")
        }
    }

    override def snapshotContent: CacheContent = {
        def toProfile(pair: (Int, Puppeteer[_ <: A])): PuppetProfile[A] = {
            val (id, puppeteer) = pair
            PuppetProfile[A](id, puppeteer.getPuppet, puppeteer.puppeteerDescription.owner)
        }

        val array = puppeteers.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

    override def flush(): this.type = {
        unFlushedPuppets.foreach(flushPuppet)
        unFlushedPuppets.clear()
        this
    }

    override var autoFlush: Boolean = true

    override def modificationCount(): Int = -1

    override def initPuppetWrapper[B <: A : ClassTag](wrapper: B with PuppetWrapper[B]): Unit = {
        if (wrapper.isInitialized)
            return
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, getPuppetDescription[B])
        wrapper.initPuppeteer(puppeteer, wrapper)
    }

    def getPuppeteer[S <: Serializable](id: Int): Option[SimplePuppeteer[S]] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer match {
                case puppeteer: SimplePuppeteer[S] => Some(puppeteer)
                case _                             => None
            }
        }
    }

    def addFieldReplacement(field: Field, replacement: Any): Unit = {
        fieldRestorer.putValue(field, replacement)
    }

    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        if (content.array.isEmpty)
            return
        val array = content.array

        array.foreach(profile => {
            val puppet = profile.puppet
            val owner  = profile.owner
            val id     = profile.id
            val puppetClassTag = ClassTag[A](puppet.getClass)
            //it's an object that must be chipped by the current objects cache (owner is the same as current identifier)
            if (owner == supportIdentifier) {
                localChips.get(id) match {
                    case None       => chipObject(id, puppet)(puppetClassTag)
                    case Some(chip) => chip.updateAllFields(puppet)
                }
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!puppeteers.contains(id)) {
                genPuppetWrapper(id, owner, puppet)(puppetClassTag)
            }
        })
    }

    private def genPuppetWrapper[B <: A : ClassTag](id: Int, owner: String, puppet: B): B with PuppetWrapper[B] = {
        ensureNotWrapped(puppet)
        val puppeteerDesc = PuppeteerDescription(family, cacheID, id, owner)
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, getPuppetDescription[B])
        puppeteers.put(id, puppeteer)

        val puppetClass = generator.getClass[B](puppet.getClass.asInstanceOf[Class[B]])
        instantiatePuppetWrapper(puppeteer, puppet, puppetClass)
    }

    private def instantiatePuppetWrapper[B <: A](puppeteer: SimplePuppeteer[B], clone: B, puppetClass: Class[B with PuppetWrapper[B]]): B with PuppetWrapper[B] = {
        val constructor = puppetClass.getDeclaredConstructor(classOf[Puppeteer[_]], clone.getClass)
        constructor.newInstance(puppeteer, clone)
    }

    private def chipObject[B <: A : ClassTag](id: Int, puppet: B): Unit = {
        val chip = ObjectChip[B](supportIdentifier, getPuppetDescription[B], puppet)
        localChips.put(id, chip)
    }

    private def flushPuppet(profile: PuppetProfile[_ <: A]): Unit = {
        AppLogger.vTrace(s"Flushing puppet $profile...")
        makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket(profile))
                .putAttribute("id", profile.id)
                .submit()
    }

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[PuppetWrapper[_]])
            throw new IllegalPuppetException("This object is already shared.")
    }

}

object CloudPuppetRepository {

    private val ClassesResourceDirectory = "/PuppetGeneration/"

    def apply[A <: Serializable : ClassTag]: SharedCacheFactory[CloudPuppetRepository[A] with InternalSharedCache] = {
        (handler: SharedCacheManager, identifier: Int, container: PacketInjectableContainer) => {
            val channel     = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            val application = handler.network.connection.getContext
            val resources   = application.getAppResources.getOrOpenShort[WrappersClassResource](ClassesResourceDirectory)
            val generator   = new PuppetWrapperClassGenerator(resources)

            new CloudPuppetRepository[A](handler, identifier, channel, generator)
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

    case class PuppetProfile[A <: Serializable](id: Int, puppet: A, owner: String) extends Serializable

    class CacheRepoContent[A <: Serializable](content: Array[PuppetProfile[A]]) extends CacheArrayContent[PuppetProfile[A]](content)

}