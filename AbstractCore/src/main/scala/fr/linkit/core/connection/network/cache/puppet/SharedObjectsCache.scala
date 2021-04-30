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

package fr.linkit.core.connection.network.cache.puppet

import fr.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.AbstractSharedCache
import fr.linkit.core.connection.network.cache.puppet.SharedObjectsCache.FieldRestorer
import fr.linkit.core.connection.network.cache.puppet.generation.PuppetWrapperClassGenerator
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.core.local.utils.ScalaUtils

import java.lang.reflect.Field
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SharedObjectsCache(handler: SharedCacheManager,
                         identifier: Int,
                         channel: RequestPacketChannel) extends AbstractSharedCache[Serializable](handler, identifier, channel) {

    private val localChips        = new mutable.HashMap[Int, ObjectChip[_ <: Serializable]]()
    private val puppeteers        = new mutable.HashMap[Int, Puppeteer[_ <: Serializable]]()
    private val unFlushedPuppets  = new mutable.HashSet[(Int, Serializable)]
    private val fieldRestorer     = new FieldRestorer
    private val supportIdentifier = channel.traffic.supportIdentifier

    AppLogger.trace(s"Shared Object cache opened (id: $identifier, family: ${handler.family}, owner: ${handler.ownerID})")

    def postCloudObject[S <: Serializable](id: Int, obj: S): S with PuppetWrapper[S] = {
        chipObject(id, obj)
        val wrapper = genPuppetObject[S](id, supportIdentifier, obj)
        flushPuppet(id, obj)
        wrapper
    }

    def findCloudObject[S <: Serializable : ClassTag](id: Int): Option[S] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer.getPuppet match {
                case _: S => Option(puppeteer.getPuppetWrapper.asInstanceOf[S])
                case _      => None
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

    def getPuppeteer[S <: Serializable](id: Int): Option[Puppeteer[S]] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer match {
                case puppeteer: Puppeteer[S] => Some(puppeteer)
                case _                       => None
            }
        }
    }

    def mapField(field: Field, value: Any): Unit = {
        fieldRestorer.putValue(field, value)
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle = ${bundle}")
        val response = bundle.packet
        val id       = response.getAttribute[Int]("id").get
        val owner    = bundle.coords.senderID

        response.nextPacket[Packet] match {
            case ObjectPacket((id: Int, puppet: Serializable)) =>
                if (!localChips.contains(id)) {
                    fieldRestorer.restoreFields(puppet)
                    genPuppetObject(id, owner, puppet)
                }

            case reqPacket => localChips.get(id.toInt).fold() { chip =>
                chip.handleBundle(reqPacket, bundle.responseSubmitter)
            }
        }
    }

    override protected def setCurrentContent(content: Array[Serializable]): Unit = {
        if (content.isEmpty)
            return
        val contentMap: Map[Int, (Serializable, String)] = ScalaUtils.slowCopy(content)
                .toMap
        contentMap.foreachEntry((id, pair) => {
            val obj   = pair._1
            val owner = pair._2
            //it's an object that must be chipped by the current objects cache
            if (owner == supportIdentifier) {
                localChips.get(id) match {
                    case None       => chipObject(id, obj)
                    case Some(chip) => chip.updateAllFields(obj)
                }
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!puppeteers.contains(id)) {
                genPuppetObject(id, pair._2, pair._1)
            }
        })
    }

    override def currentContent: Array[Any] = {
        puppeteers
                .map(pair => (pair._1, (pair._2.getPuppet, pair._2.owner)))
                .toArray
    }

    override var autoFlush: Boolean = true

    override def flush(): this.type = {
        unFlushedPuppets.foreach(pair => flushPuppet(pair._1, pair._2))
        unFlushedPuppets.clear()
        this
    }

    override def modificationCount(): Int = -1

    override def link(action: Serializable => Unit): this.type = {
        links += action
        localChips.foreachEntry((_, chip) => action(chip.puppet))
        unFlushedPuppets.foreach(pair => action(pair._2))
        this
    }

    private def genPuppetObject[S <: Serializable](id: Int, owner: String, puppet: S): S with PuppetWrapper[S] = {
        val fields    = PuppetClassFields.ofRef(puppet)
        val puppeteer = new Puppeteer[S](channel, this, id, owner, fields)
        puppeteers.put(id, puppeteer)

        val puppetClass = PuppetWrapperClassGenerator.getOrGenerate[S](puppet.getClass)
        instantiatePuppet[S](puppeteer, puppet, puppetClass)
    }

    private def instantiatePuppet[S <: Serializable](puppeteer: Puppeteer[S], clone: S, puppetClass: Class[S with PuppetWrapper[S]]): S with PuppetWrapper[S] = {
        val constructor = puppetClass.getConstructor(puppeteer.getClass, clone.getClass)
        constructor.newInstance(puppeteer, clone)
    }

    private def chipObject[S <: Serializable](id: Int, puppet: S): Unit = {
        val chip = ObjectChip[S](supportIdentifier, puppet)
        localChips.put(id, chip)
    }

    private def flushPuppet(id: Int, puppet: Serializable): Unit = {
        AppLogger.vTrace(s"Flushing puppet $puppet for id $id...")
        makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket((id, puppet)))
                .putAttribute("id", id)
                .submit()
    }

}

object SharedObjectsCache extends SharedCacheFactory[SharedObjectsCache] {

    override def createNew(handler: SharedCacheManager,
                           identifier: Int, baseContent: Array[Any],
                           container: PacketInjectableContainer): SharedObjectsCache = {
        val channel = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
        val cache   = new SharedObjectsCache(handler, identifier, channel)
        cache.setCurrentContent(ScalaUtils.slowCopy[Serializable](baseContent))
        cache
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

}
