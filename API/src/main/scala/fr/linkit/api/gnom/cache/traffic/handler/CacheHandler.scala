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

import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle

/**
 * The main class that handles the network operations of a cache.
 * His behavior may have some differences depending on what cache instance this handler handles.
 * (May have more privileges if the cache handler handles a cache where its manager is owned by the current engine id)
 * @see [[CacheAttachHandler]]
 * @see [[CacheContentHandler[_]]
 * */
trait CacheHandler {

    /**
     * Handles a request packet bundle
     * @param bundle the request to handle.
     * */
    def handleBundle(bundle: RequestPacketBundle): Unit

}
