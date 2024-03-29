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

import fr.linkit.api.gnom.referencing.NetworkObjectReference

class SharedCacheManagerReference(val family: String) extends NetworkObjectReference {

    override def asSuper: Option[NetworkObjectReference] = None

    override def toString: String = {
        s"@network/caches/$family"
    }

    override def hashCode(): Int = family.hashCode

    override def equals(obj: Any): Boolean = obj match {
        case ref: SharedCacheManagerReference => ref.family == family
        case _ => false
    }

    def / (cacheID: Int): SharedCacheReference = new SharedCacheReference(family, cacheID)
}
