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

package fr.linkit.api.gnom.cache

import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.DynamicNetworkObject

/**
 * The shared cache trait that depicts the information a cache must contain
 * In order to be correctly managed by its [[SharedCacheManager]]
 */
trait SharedCache extends DynamicNetworkObject[SharedCacheReference] {

    val info: CacheInfo

}

object SharedCache {
    case class CacheInfo(family: String, cacheID: Int, ownerTag: UniqueTag with NetworkFriendlyEngineTag)
}
