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

import fr.linkit.api.connection.cache.traffic.CachePacketChannel
import fr.linkit.api.connection.cache.traffic.handler.{CacheHandler, ContentHandler}
import fr.linkit.api.connection.cache.{CacheContent, SharedCacheManager}
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacketChannel

class DefaultCachePacketChannel(scope: ChannelScope,
                                override val manager: SharedCacheManager,
                                override val cacheID: Int) extends RequestPacketChannel(null, scope) with CachePacketChannel {

    private var handler: Option[CacheHandler] = None

    override def getCacheOfOwner: CacheContent = {

    }

    override def setHandler(handler: CacheHandler): Unit = {
        this.handler = Option(handler)
    }

    def getHandler: Option[CacheHandler] = handler
}
