/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.traffic.handler

import fr.linkit.api.gnom.cache.{CacheContent, SharedCacheReference}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH

/**
 * Handles the local content of the cache instance. <br>
 * <u>This handler must not affect the content of other remote caches.
 * it only affects the local content of the cache it handles.</u>
 * @tparam C the type of content that must be set or get.
 * */
trait CacheContentHandler[C <: CacheContent] extends CacheHandler {

    /**
     * Sets the local content
     * @param content the content that must be set.
     * */
    def initializeContent(content: C): Unit

    val lazyContentHandling: Boolean
    
    /**
     * The initial content sent to engines that connects to the cache.
     * */
    def getInitialContent: C
    
    /**
     * @return the content contained in the cache.
     *         if lazyContentHandling is set to true, the returned content can be empty, or may not contain some elements
     *         that are present in the cache.
     * */
    def getContent: C

    /**
     * Note: This method is only called if the handler handles a cache where its manager handles itself.
     * @return true if the engine can access to the content, false instead
     * */
    def canAccessToContent(engine: Engine): Boolean = true

    val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH]

}
