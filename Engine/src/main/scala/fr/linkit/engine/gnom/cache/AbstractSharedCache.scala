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

import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheReference}
import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence

abstract class AbstractSharedCache(channel: CachePacketChannel) extends AbstractAttributesPresence with SharedCache {

    private val manager = channel.manager

    override val family: String = manager.family

    override val ownerTag: UniqueTag with NetworkFriendlyEngineTag = channel.ownerTag
    override val cacheID : Int                                     = channel.cacheID
    override val reference: SharedCacheReference                    = new SharedCacheReference(family, cacheID)
    override val presence : NetworkObjectPresence                   = manager.getCachesLinker.getPresence(reference)

}
