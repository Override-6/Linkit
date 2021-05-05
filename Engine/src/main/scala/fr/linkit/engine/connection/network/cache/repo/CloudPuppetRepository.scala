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

package fr.linkit.engine.connection.network.cache.repo

import fr.linkit.api.connection.network.cache.repo._
import fr.linkit.api.connection.network.cache.{CacheContent, InternalSharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.network.cache.repo.CloudPuppetRepository.{CacheRepoContent, FieldRestorer, PuppetProfile}
import fr.linkit.engine.connection.network.cache.repo.generation.PuppetWrapperClassGenerator
import fr.linkit.engine.connection.network.cache.{AbstractSharedCache, CacheArrayContent}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.utils.ScalaUtils
import java.lang.reflect.Field

import fr.linkit.api.connection.network.cache.repo.generation.PuppeteerDescription

import scala.collection.mutable
import scala.reflect.ClassTag

class CloudPuppetRepository[A <: Serializable](handler: SharedCacheManager,
                                               cacheID: Int,
                                               channel: RequestPacketChannel)(implicit aCt: ClassTag[A])
        extends AbstractSharedCache(handler, cacheID, channel) with PuppetRepository[A] {

    private val localChips        = new mutable.HashMap[Int, ObjectChip[A]]()
    private val puppeteers        = new mutable.HashMap[Int, SimplePuppeteer[A]]()
    private val unFlushedPuppets  = new mutable.HashSet[PuppetProfile[A]]
    private val fieldRestorer     = new FieldRestorer
    private val supportIdentifier = channel.traffic.supportIdentifier

    AppLogger.vTrace(s"Shared Object cache opened (id: $cacheID, family: ${handler.family}, owner: ${handler.ownerID})")

    override def postObject(id: Int, obj: A): A with PuppetWrapper[A] = {
        ensureNotWrapped(obj)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        chipObject(id, obj)
        flushPuppet(PuppetProfile(id, obj, supportIdentifier))
        genPuppetWrapper(id, supportIdentifier, obj)
    }

    override def findObject(id: Int): Option[A with PuppetWrapper[A]] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer.getPuppet match {
                case _: A => Option(puppeteer.getPuppetWrapper)
                case _    => None
            }
        }
    }

    override def isRegistered(identifier: Int): Boolean = {
        puppeteers.contains(identifier) || unFlushedPuppets.exists(_.id == identifier)
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

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle = ${bundle}")
        val response = bundle.packet
        val id       = response.getAttribute[Int]("id").get
        val owner    = bundle.coords.senderID

        response.nextPacket[Packet] match {
            case ObjectPacket(profile: PuppetProfile[A]) =>
                if (!localChips.contains(id)) {
                    val puppet = profile.puppet
                    fieldRestorer.restoreFields(puppet)
                    genPuppetWrapper(id, owner, puppet)
                }

            case reqPacket => localChips.get(id.toInt).fold() { chip =>
                chip.handleBundle(reqPacket, bundle.responseSubmitter)
            }
        }
    }

    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        val array = content.array
        if (array.isEmpty)
            return

        array.foreach(profile => {
            val puppet = profile.puppet
            val owner  = profile.owner
            val id     = profile.id
            //it's an object that must be chipped by the current objects cache (owner is the same as current identifier)
            if (owner == supportIdentifier) {
                localChips.get(id) match {
                    case None       => chipObject(id, puppet)
                    case Some(chip) => chip.updateAllFields(puppet)
                }
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!puppeteers.contains(id)) {
                genPuppetWrapper(id, owner, puppet)
            }
        })
    }

    override def setContent(content: CacheContent): Unit = {
        content match {
            case content: CacheRepoContent[A] => setRepoContent(content)
            case _                            => throw new IllegalArgumentException(s"Received unknown content '$content'")
        }
    }

    override def snapshotContent: CacheContent = {
        def toProfile(pair: (Int, SimplePuppeteer[A])): PuppetProfile[A] = {
            val (id, puppeteer) = pair
            PuppetProfile[A](id, puppeteer.getPuppet, puppeteer.description.owner)
        }

        val array = puppeteers.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

    override var autoFlush: Boolean = true

    override def flush(): this.type = {
        unFlushedPuppets.foreach(flushPuppet)
        unFlushedPuppets.clear()
        this
    }

    override def modificationCount(): Int = -1

    override def initPuppetWrapper(wrapper: A with PuppetWrapper[A]): Unit = {
        if (wrapper.isInitialized)
            return
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val puppetDesc    = PuppetClassDesc.ofClass(wrapper.getClass.getSuperclass)
        val puppeteer     = new SimplePuppeteer[A](channel, this, puppeteerDesc, puppetDesc)
        wrapper.initPuppeteer(puppeteer, wrapper)
    }

    private def genPuppetWrapper(id: Int, owner: String, puppet: A): A with PuppetWrapper[A] = {
        ensureNotWrapped(puppet)
        val puppetDesc    = PuppetClassDesc.ofRef(puppet)
        val puppeteerDesc = PuppeteerDescription(family, cacheID, id, owner)
        val puppeteer     = new SimplePuppeteer[A](channel, this, puppeteerDesc, puppetDesc)
        puppeteers.put(id, puppeteer)

        val puppetClass = PuppetWrapperClassGenerator.getOrGenerate[A](puppet.getClass)
        instantiatePuppetWrapper(puppeteer, puppet, puppetClass)
    }

    private def instantiatePuppetWrapper(puppeteer: SimplePuppeteer[A], clone: A, puppetClass: Class[A with PuppetWrapper[A]]): A with PuppetWrapper[A] = {
        /*println(s"puppeteer = ${puppeteer}")
        println(s"puppetClass = ${puppetClass}")
        puppetClass.getDeclaredConstructors.foreach(println)
        puppetClass.getDeclaredMethods.foreach(println)
        puppetClass.getDeclaredFields.foreach(println)*/
        val constructor = puppetClass.getDeclaredConstructor(classOf[Puppeteer[_]], clone.getClass)
        constructor.newInstance(puppeteer, clone)
    }

    private def chipObject(id: Int, puppet: A): Unit = {
        val chip = ObjectChip[A](supportIdentifier, puppet)
        localChips.put(id, chip)
    }

    private def flushPuppet(profile: PuppetProfile[A]): Unit = {
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

    def apply[A <: Serializable : ClassTag]: SharedCacheFactory[CloudPuppetRepository[A] with InternalSharedCache] = (handler: SharedCacheManager,
                                                                                                                      identifier: Int,
                                                                                                                      container: PacketInjectableContainer) => {
        val channel = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
        new CloudPuppetRepository[A](handler, identifier, channel)
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
