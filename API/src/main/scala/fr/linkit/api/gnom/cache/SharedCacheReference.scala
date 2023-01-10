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

import fr.linkit.api.gnom.referencing.NamedIdentifier
import java.util

class SharedCacheReference(family: String, val cacheID: Int) extends SharedCacheManagerReference(family) {

    override def asSuper: Option[SharedCacheManagerReference] = Some(new SharedCacheManagerReference(family))

    override def toString: String = s"@network/caches/$family/$cacheID"

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(family, cacheID))

    override def equals(obj: Any): Boolean = obj match {
        case ref: SharedCacheReference => ref.cacheID == cacheID && ref.family == family
        case _                         => false
    }

}