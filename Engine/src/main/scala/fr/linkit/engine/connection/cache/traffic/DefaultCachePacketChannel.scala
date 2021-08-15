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

package fr.linkit.engine.connection.cache.traffic

import fr.linkit.api.connection.cache.{CacheContent, CacheSearchBehavior, NoSuchCacheException, SharedCacheManager}
import fr.linkit.api.connection.cache.traffic.CachePacketChannel
import fr.linkit.api.connection.cache.traffic.handler.CacheHandler
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.PacketInjectableFactory
import fr.linkit.engine.connection.packet.traffic.channel.request.SimpleRequestPacketChannel

class DefaultCachePacketChannel(scope: ChannelScope,
                                parent: PacketChannel,
                                override val manager: SharedCacheManager,
                                override val cacheID: Int) extends SimpleRequestPacketChannel(parent, scope) with CachePacketChannel {

    private var handler: Option[CacheHandler] = None

    override def getCacheOfOwner: CacheContent = {
        manager.retrieveCacheContent(cacheID, CacheSearchBehavior.GET_OR_CRASH).getOrElse {
            throw new NoSuchCacheException(s"No content was found for cache $cacheID.")
        }
    }

    override def setHandler(handler: CacheHandler): Unit = {
        if (this.handler.isDefined)
            throw new IllegalStateException("Handler is already defined !")
        if (handler == null)
            throw new NullPointerException("Handler is null.")
        this.handler = Some(handler)
        addRequestListener(handler.handleBundle)
    }

    def getHandler: Option[CacheHandler] = handler

}

object DefaultCachePacketChannel {

    def apply(cacheID: Int, manager: SharedCacheManager): PacketInjectableFactory[CachePacketChannel] = (parent: PacketChannel, scope: ChannelScope) => {
        new DefaultCachePacketChannel(scope, parent, manager, cacheID)
    }
}
