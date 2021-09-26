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

package fr.linkit.engine.connection.cache

import fr.linkit.api.connection.cache.traffic.CachePacketChannel
import fr.linkit.api.connection.cache.traffic.handler.{CacheHandler, ContentHandler}
import fr.linkit.api.connection.cache.{CacheContent, SharedCacheFactory}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.RequestPacketBundle
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.SharedInstance.CacheInstanceContent
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.local.utils.{ConsumerContainer, JavaUtils}

import scala.reflect.ClassTag

class SharedInstance[A <: Serializable : ClassTag] private(channel: CachePacketChannel)
        extends AbstractSharedCache(channel) {

    private val listeners = new ConsumerContainer[Option[A]]

    channel.setHandler(SharedInstanceHandler)

    @volatile private var modCount         = 0
    @volatile private var value: Option[A] = None

    override def toString: String = s"SharedInstance(${value.orNull})"

    def get: Option[A] = value

    def apply: Option[A] = value


    def set(t: A): this.type = {
        value = Option(t)
        modCount += 1
        listeners.applyAll(value)
        AppLogger.vTrace(s"INSTANCE IS NOW (local) : $value")
        flush()
    }

    private def flush(): this.type = {
        channel.makeRequest(ChannelScopes.broadcast)
                .addPacket(ObjectPacket(value.orNull))
                .submit()
                .detach()
        this
    }

    def addListener(callback: Option[A] => Unit): this.type = {
        listeners += callback
        this
    }

    private object SharedInstanceHandler extends CacheHandler with ContentHandler[CacheInstanceContent[A]] {

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            AppLogger.vTrace(s"<$family> Handling packet $bundle")
            bundle.packet.nextPacket[Packet] match {
                case ObjectPacket(remoteInstance: A) =>
                    value = Option(remoteInstance)
                    modCount += 1
                    listeners.applyAll(value)
                    AppLogger.vTrace(s"<$family> INSTANCE IS NOW (network): $value")

                case _ => throw UnexpectedPacketException("Unable to handle a non ObjectPacket into SharedInstance")
            }
        }

        override def initializeContent(content: CacheInstanceContent[A]): Unit = {
            value = Option(content.value)
        }

        override def getContent: CacheInstanceContent[A] = {
            val v: A = if (value.isDefined) value.get else JavaUtils.getNull[A]
            CacheInstanceContent(v)
        }
    }
}

object SharedInstance {

    def apply[A <: Serializable : ClassTag]: SharedCacheFactory[SharedInstance[A]] = {
        (channel: CachePacketChannel) => {
            new SharedInstance[A](channel)
        }
    }

    case class CacheInstanceContent[A](value: A) extends CacheContent

}