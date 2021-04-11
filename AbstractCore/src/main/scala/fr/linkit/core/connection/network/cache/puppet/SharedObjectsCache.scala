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
import fr.linkit.core.connection.network.cache.puppet.generation.PuppetClassGenerator
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}

import scala.collection.mutable

class SharedObjectsCache(handler: SharedCacheManager,
                         identifier: Long,
                         channel: RequestPacketChannel) extends AbstractSharedCache[Serializable](handler, identifier, channel) {

    private val localChips        = new mutable.HashMap[Long, ObjectChip[_ <: Serializable]]()
    private val puppeteers        = new mutable.HashMap[Long, Puppeteer[_ <: Serializable]]()
    private val unFlushedPuppets  = new mutable.HashSet[(Long, Serializable)]
    private val supportIdentifier = channel.traffic.supportIdentifier

    def postCloudObject[S <: Serializable](id: Long, obj: S): S with PuppetObject = {
        chipObject(id, obj)
        genPuppetObject[S](id, supportIdentifier, obj)
    }

    def findCloudObject[P <: PuppetObject](id: Long): Option[P] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer match {
                case pup: P => Option(pup)
                case _      => None
            }
        }
    }

    def getChip[S <: Serializable](id: Long): Option[ObjectChip[S]] = {
        localChips.get(id) match {
            case None       => None
            case Some(chip) => chip match {
                case chip: ObjectChip[S] => Some(chip)
                case _                   => None
            }
        }
    }

    def getPuppeteer[S <: Serializable](id: Long): Option[Puppeteer[S]] = {
        puppeteers.get(id) match {
            case None            => None
            case Some(puppeteer) => puppeteer match {
                case puppeteer: Puppeteer[S] => Some(puppeteer)
                case _                       => None
            }
        }
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle = ${bundle}")
        val response = bundle.packet
        val id       = response.getAttribute[Long]("id").get
        val owner    = bundle.coords.senderID

        response.nextPacket[Packet] match {
            case ObjectPacket((id: Long, puppet: Serializable)) =>
                if (!localChips.contains(id))
                    genPuppetObject(id, owner, puppet)

            case reqPacket => localChips.get(id.toLong).fold() { chip =>
                chip.handleBundle(reqPacket, bundle.responseSubmitter)
            }
        }
    }

    override protected def setCurrentContent(content: Array[Serializable]): Unit = {
        val contentMap = content.asInstanceOf[Array[(Long, (Serializable, String))]]
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
        val any1 = localChips
                .map(pair => (pair._1, (pair._2.puppet, pair._2.owner)))
                .toArray
        val any2 = puppeteers.map(pair => (pair._1, (pair._2.getPuppet, pair._2.owner)))
                .toArray
        (any1 ++ any2).asInstanceOf[Array[Any]]
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

    private def genPuppetObject[S <: Serializable](id: Long, owner: String, puppet: S): S with PuppetObject = {
        val fields    = PuppetClassFields.ofRef(puppet)
        val puppeteer = new Puppeteer[S](channel, this, id, owner, fields)
        puppeteers.put(id, puppeteer)
        flushPuppet(id, puppet)

        val puppetClass = PuppetClassGenerator.getOrGenerate[S](puppet.getClass)
        instantiatePuppet[S](puppeteer, puppet, puppetClass)
    }

    private def instantiatePuppet[S <: Serializable](puppeteer: Puppeteer[S], clone: S, puppetClass: Class[S with PuppetObject]): S with PuppetObject = {
        val constructor = puppetClass.getConstructor(puppeteer.getClass, clone.getClass)
        constructor.newInstance(puppeteer, clone)
    }

    private def chipObject[S <: Serializable](id: Long, puppet: S): Unit = {
        val chip = ObjectChip[S](supportIdentifier, puppet)
        localChips.put(id, chip)
    }

    private def flushPuppet(id: Long, puppet: Serializable): Unit = {
        AppLogger.vTrace(s"Flushing puppet $puppet for id $id...")
        makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket((id, puppet)))
                .putAttribute("id", id)
                .submit()
    }

}

object SharedObjectsCache extends SharedCacheFactory[SharedObjectsCache] {

    override def createNew(handler: SharedCacheManager,
                           identifier: Long, baseContent: Array[Any],
                           container: PacketInjectableContainer): SharedObjectsCache = {
        val channel = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
        new SharedObjectsCache(handler, identifier, channel)
    }

}
