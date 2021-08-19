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

package fr.linkit.api.connection.cache.traffic

import fr.linkit.api.connection.cache.{CacheContent, SharedCacheManager}
import fr.linkit.api.connection.cache.traffic.handler.CacheHandler
import fr.linkit.api.connection.packet.channel.request.RequestPacketChannel

/**
 * This channel is given to any [[fr.linkit.api.connection.cache.SharedCache]]
 * (according to [[fr.linkit.api.connection.cache.SharedCacheFactory]])
 * and performs all the request sending and reception.<br>
 * a [[CacheHandler]] can be set in order to complete requests or to perform
 * content handling ([[fr.linkit.api.connection.cache.traffic.handler.ContentHandler]]), or
 * choose and get any information about the engines that are attaching to the cache ([[fr.linkit.api.connection.cache.traffic.handler.AttachHandler]]).
 *
 * @see [[CacheHandler]]
 * @see [[fr.linkit.api.connection.cache.traffic.handler.AttachHandler]]
 * @see [[fr.linkit.api.connection.cache.traffic.handler.ContentHandler]]
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
     * @return the original content of the cache.
     */
    def getCacheOfOwner: CacheContent

    /**
     *
     * @return Some(CacheHandler) if the handler is set, None instead
     */
    def getHandler: Option[CacheHandler]

}
