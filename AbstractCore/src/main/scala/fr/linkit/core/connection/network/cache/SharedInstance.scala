/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.network.cache

import fr.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.traffic.{PacketSender, PacketSyncReceiver}
import fr.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.linkit.core.connection.packet.UnexpectedPacketException
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.local.utils.ConsumerContainer

import scala.reflect.ClassTag

class SharedInstance[A <: Serializable : ClassTag] private(handler: SharedCacheManager,
                                                           identifier: Long,
                                                           channel: PacketSender with PacketSyncReceiver)
        extends AbstractSharedCache[A](handler, identifier, channel) {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]

    @volatile private var modCount            = 0
    @volatile private var instance: Option[A] = None

    def this(handler: SharedCacheManager,
             identifier: Long,
             channel: PacketSender with PacketSyncReceiver,
             value: A = null) = {
        this(handler, identifier, channel)
        instance = Option(value)
    }

    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        //println(s"<$family> Handling packet $packet")
        packet match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = Option(remoteInstance)
                modCount += 1
                listeners.applyAll(remoteInstance)
            //println(s"<$family> INSTANCE IS NOW (network): $instance")

            case _ => throw UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    }

    override def modificationCount(): Int = modCount

    override def currentContent: Array[Any] = Array(instance.orNull)

    override def toString: String = s"SharedInstance($instance)"

    override protected def setCurrentContent(content: Array[A]): Unit = {
        content.ensuring(_.length <= 1)
        if (content.isEmpty) {
            instance = None
            return
        }
        instance = Option(content(0))
    }

    def get: Option[A] = instance

    def set(t: A): this.type = {
        instance = Option(t)
        modCount += 1
        listeners.applyAll(t)
        //println(s"INSTANCE IS NOW (local) : $instance $autoFlush")
        if (autoFlush)
            flush()
        this
    }

    override def flush(): this.type = {
        sendRequest(ObjectPacket(instance))
        this
    }

    def addListener(callback: A => Unit): this.type = {
        listeners += callback
        this
    }
}

object SharedInstance {

    def apply[A <: Serializable : ClassTag]: SharedCacheFactory[SharedInstance[A]] = {
        (handler: SharedCacheManager, identifier: Long, baseContent: Array[Any], channel: PacketSender with PacketSyncReceiver) => {
            if (baseContent.isEmpty)
                new SharedInstance[A](handler, identifier, channel)
            else new SharedInstance[A](handler, identifier, channel, baseContent(0).asInstanceOf[A])
        }
    }

}