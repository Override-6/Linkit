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

package fr.linkit.api.connection.cache.traffic.handler

import fr.linkit.api.connection.cache.CacheContent
import fr.linkit.api.connection.network.Engine

/**
 * Handles the local content of the cache instance. <br>
 * <u>This handler must not affect the content of other remote caches.
 * it only affects the local content of the cache it handles.</u>
 * @tparam C the type of content that must be set or get.
 * */
trait ContentHandler[C <: CacheContent] extends CacheHandler {

    /**
     * Sets the local content
     * @param content the content that must be set.
     * */
    def initializeContent(content: C): Unit

    /**
     * @return C the local content of this cache.
     * */
    def getContent: C

    /**
     * Note: This method is only called if the handler handles a cache where its manager handles itself.
     * @return true if the engine can access to the content, false instead
     * */
    def canAccessToContent(engine: Engine): Boolean = true

}
