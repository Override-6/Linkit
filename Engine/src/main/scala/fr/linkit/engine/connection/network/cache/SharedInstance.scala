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

package fr.linkit.engine.connection.network.cache

import fr.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.utils.ConsumerContainer

import scala.reflect.ClassTag

class SharedInstance[A <: Serializable : ClassTag] private(handler: SharedCacheManager,
                                                           identifier: Int,
                                                           channel: RequestPacketChannel)
        extends AbstractSharedCache[A](handler, identifier, channel) {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]

    @volatile private var modCount            = 0
    @volatile private var instance: Option[A] = None

    def this(handler: SharedCacheManager,
             identifier: Int,
             channel: RequestPacketChannel,
             value: A = null) = {
        this(handler, identifier, channel)
        instance = Option(value)
    }

    override def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vTrace(s"<$family> Handling packet $bundle")
        bundle.packet.nextPacket[Packet] match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = Option(remoteInstance)
                modCount += 1
                listeners.applyAll(remoteInstance)
                AppLogger.vTrace(s"<$family> INSTANCE IS NOW (network): $instance")

            case _ => throw UnexpectedPacketException("Unable to handle a non ObjectPacket into SharedInstance")
        }
    }

    override def modificationCount(): Int = modCount

    override def currentContent: Array[Any] = Array(instance.orNull)

    override def toString: String = s"SharedInstance(${instance.orNull})"

    override def link(action: A => Unit): this.type = {
        links += action
        action(instance.getOrElse(null.asInstanceOf[A]))
        this
    }

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
        AppLogger.vTrace(s"INSTANCE IS NOW (local) : $instance $autoFlush")
        if (autoFlush)
            flush()
        this
    }

    override def flush(): this.type = {
        sendModification(ObjectPacket(instance.orNull))
        this
    }

    def addListener(callback: A => Unit): this.type = {
        listeners += callback
        this
    }
}

object SharedInstance {

    def apply[A <: Serializable : ClassTag]: SharedCacheFactory[SharedInstance[A]] = {
        (handler: SharedCacheManager, identifier: Int, baseContent: Array[Any], container: PacketInjectableContainer) => {
            val channel = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            if (baseContent.isEmpty)
                new SharedInstance[A](handler, identifier, channel)
            else new SharedInstance[A](handler, identifier, channel, baseContent(0).asInstanceOf[A])
        }
    }

}