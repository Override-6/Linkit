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

package fr.linkit.api.gnom.cache.traffic

import fr.linkit.api.gnom.cache.{CacheContent, SharedCacheManager, SharedCacheReference}
import fr.linkit.api.gnom.cache.traffic.handler.CacheHandler
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel

/**
 * This channel is given to any [[fr.linkit.api.gnom.cache.SharedCache]]
 * (according to [[fr.linkit.api.gnom.cache.SharedCacheFactory]])
 * and performs all the request sending and reception.<br>
 * a [[CacheHandler]] can be set in order to complete requests or to perform
 * content handling ([[fr.linkit.api.gnom.cache.traffic.handler.CacheContentHandler]]), or
 * choose and get any information about the engines that are attaching to the cache ([[fr.linkit.api.gnom.cache.traffic.handler.CacheAttachHandler]]).
 *
 * @see [[CacheHandler]]
 * @see [[fr.linkit.api.gnom.cache.traffic.handler.CacheAttachHandler]]
 * @see [[fr.linkit.api.gnom.cache.traffic.handler.CacheContentHandler]]
 */
trait CachePacketChannel extends RequestPacketChannel {

    /**
     * The manager of this channel's cache.
     */
    val manager: SharedCacheManager


    /**
     * This cache's identifier
     */
    val cacheID: Int

    /**
     * sets the cache handler.
     *
     * @param handler the cache's handler
     * @throws IllegalStateException if the handler is already set.
     */
    def setHandler(handler: CacheHandler): Unit
    /**
     *
     * @return Some(CacheHandler) if the handler is set, None instead
     */
    def getHandler: Option[CacheHandler]


}
