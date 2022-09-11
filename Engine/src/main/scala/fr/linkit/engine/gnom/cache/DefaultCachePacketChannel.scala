/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.CacheHandler
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic.{PacketInjectableFactory, PacketInjectableStore}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.channel.request.SimpleRequestPacketChannel

class DefaultCachePacketChannel(scope: ChannelScope,
                                store: PacketInjectableStore,
                                override val manager: SharedCacheManager,
                                override val cacheID: Int) extends SimpleRequestPacketChannel(store, scope) with CachePacketChannel {

    private var handler: Option[CacheHandler] = None

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

    def apply(cacheID: Int, manager: SharedCacheManager): PacketInjectableFactory[CachePacketChannel] = (store: PacketInjectableStore, scope: ChannelScope) => {
        new DefaultCachePacketChannel(scope, store, manager, cacheID)
    }
}