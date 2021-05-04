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

package fr.linkit.api.connection.network.cache

import fr.linkit.api.connection.network.Updatable

import scala.reflect.ClassTag

trait SharedCacheManager extends Updatable {

    val family : String
    val ownerID: String

    def postInstance[A <: Serializable](key: Int, value: A): A

    def getInstance[A <: Serializable](key: Int): Option[A]

    def getInstanceOrWait[A <: Serializable](key: Int): A

    def apply[A <: Serializable](key: Int): A

    def getCache[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A with InternalSharedCache], behavior: CacheOpenBehavior = CacheOpenBehavior.GET_OR_EMPTY): A

    def getUpdated[A <: SharedCache : ClassTag](cacheID: Int, factory: SharedCacheFactory[A with InternalSharedCache], behavior: CacheOpenBehavior = CacheOpenBehavior.GET_OR_EMPTY): A

    def retrieveCacheContent(cacheID: Int, behavior: CacheOpenBehavior): Option[CacheContent]

}
