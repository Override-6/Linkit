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

import fr.linkit.api.connection.network.cache.{CacheOpenBehavior, InternalSharedCache, SharedCacheManager}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.RequestPacketChannel
import fr.linkit.core.local.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

abstract class AbstractSharedCache[A <: Serializable : ClassTag](@Nullable handler: SharedCacheManager,
                                                                 identifier: Long,
                                                                 channel: RequestPacketChannel) extends InternalSharedCache with JustifiedCloseable {

    override val family: String = if (handler == null) "" else handler.family

    override def close(reason: Reason): Unit = channel.close(reason)

    override def isClosed: Boolean = channel.isClosed

    override def update(): this.type = {
        if (handler == null)
            return this

        //println(s"<$family> UPDATING CACHE $identifier")
        val content = handler.retrieveCacheContent(identifier, CacheOpenBehavior.GET_OR_CRASH)
        //println(s"<$family> RECEIVED UPDATED CONTENT FOR CACHE $identifier : ${content.mkString("Array(", ", ", ")")}")

        setCurrentContent(ScalaUtils.slowCopy(content))
        this
    }

    protected def sendModification(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        val request = channel
                .makeRequest(ChannelScopes.broadcast)
                .addPacket(packet)
                .putAttribute("family", family)
                .putAttribute("cache", identifier)
        attributes.drainAttributes(request)
        request.submit()
    }

    protected def setCurrentContent(content: Array[A]): Unit

}
