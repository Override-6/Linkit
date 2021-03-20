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

package fr.`override`.linkit.core.connection.network.cache

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.core.connection.packet
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel

import scala.reflect.ClassTag

class SharedInstance[A <: Serializable : ClassTag] private(handler: AbstractSharedCacheManager,
                                                           identifier: Long,
                                                           channel: traffic.channel.CommunicationPacketChannel)
        extends HandleableSharedCache[A](handler, identifier, channel) {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]

    @volatile private var modCount = 0
    @volatile private var instance: A = _

    def this(handler: AbstractSharedCacheManager,
             identifier: Long,
             channel: packet.traffic.channel.CommunicationPacketChannel,
             value: A = null) = {
        this(handler, identifier, channel)
        instance = value
    }

    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        //println(s"<$family> Handling packet $packet")
        packet match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = remoteInstance
                modCount += 1
                listeners.applyAll(remoteInstance)
            //println(s"<$family> INSTANCE IS NOW (network): $instance")

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    }

    override def modificationCount(): Int = modCount

    override def currentContent: Array[Any] = Array(instance)

    override protected def setCurrentContent(content: Array[A]): Unit = {
        content.ensuring(_.length == 1)
        set(content(0))
    }

    def get: A = instance

    def set(t: A): this.type = {
        instance = t
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
        (handler: AbstractSharedCacheManager, identifier: Long, baseContent: Array[Any], channel: channel.CommunicationPacketChannel) => {
            if (baseContent.isEmpty)
                new SharedInstance[A](handler, identifier, channel)
            else new SharedInstance[A](handler, identifier, channel, baseContent(0).asInstanceOf[A])
        }
    }

}