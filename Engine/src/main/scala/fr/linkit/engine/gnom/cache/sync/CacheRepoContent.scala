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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.engine.gnom.cache.CacheArrayContent
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.FirstFloorObjectProfile

class CacheRepoContent[A <: AnyRef](content: Array[FirstFloorObjectProfile[A]]) extends CacheArrayContent[FirstFloorObjectProfile[A]](content)

object CacheRepoContent {

    def apply[A <: AnyRef](content: Array[FirstFloorObjectProfile[A]]): CacheRepoContent[A] = new CacheRepoContent(content)
}