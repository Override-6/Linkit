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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence

abstract class AbstractSharedCache(channel: CachePacketChannel) extends AbstractAttributesPresence with SharedCache {

    private val manager = channel.manager

    override val family   : String                = manager.family
    override val cacheID  : Int                   = channel.cacheID
    override val reference: SharedCacheReference  = new SharedCacheReference(family, cacheID)
    override val presence : NetworkObjectPresence = manager.getCachesLinker.findPresence(reference).get

}
