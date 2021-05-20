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

import fr.linkit.api.connection.cache.{CacheContent, CacheOpenBehavior, InternalSharedCache, SharedCacheManager}
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel, RequestSubmitter}
import fr.linkit.engine.connection.packet.{AbstractAttributesPresence, SimplePacketAttributes}
import org.jetbrains.annotations.Nullable

abstract class AbstractSharedCache(@Nullable handler: SharedCacheManager,
                                   identifier: Int,
                                   channel: RequestPacketChannel) extends AbstractAttributesPresence with InternalSharedCache with JustifiedCloseable {

    override val family: String = if (handler == null) "" else handler.family

    override def close(reason: Reason): Unit = channel.close(reason)

    override def isClosed: Boolean = channel.isClosed

    override def update(): this.type = {
        if (handler == null)
            return this

        //println(s"<$family> UPDATING CACHE $identifier")
        val content = handler.retrieveCacheContent(identifier, CacheOpenBehavior.GET_OR_CRASH)
        //println(s"<$family> RECEIVED UPDATED CONTENT FOR CACHE $identifier : ${content.mkString("Array(", ", ", ")")}")
        if (content.isDefined) {
            setContent(content.get)
        }
        this
    }

    //def link(action: A => Unit): this.type

    protected def handleBundle(bundle: RequestBundle): Unit

    protected def sendModification(packet: Packet, attributes: PacketAttributes = SimplePacketAttributes.empty): Unit = {
        val request = makeRequest(ChannelScopes.discardCurrent)
                .addPacket(packet)
        attributes.drainAttributes(request)
        request.submit()
    }

    protected def makeRequest(scopeFactory: ScopeFactory[_ <: ChannelScope]): RequestSubmitter = {
        val request = channel.makeRequest(scopeFactory)
        drainAllAttributes(request)
        request
    }

    addDefaultAttribute("family", family)
    addDefaultAttribute("cache", identifier)

    //FIXME optimise (find another way to find the right cache that can accept the bundle)
    channel.addRequestListener(bundle => {
        val attr = bundle.attributes

        def isPresent(name: String, expected: Any): Boolean = attr.getAttribute(name).contains(expected)

        if (isPresent("cache", identifier) && isPresent("family", family))
            handleBundle(bundle)
    })

}
