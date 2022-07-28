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

package fr.linkit.api.gnom.cache

import fr.linkit.api.gnom.cache.traffic.CachePacketChannel

import scala.reflect.{ClassTag, classTag}

/**
 * Used by the [[SharedCacheManager]] to create a [[SharedCache]] of type [[A]]
 * @tparam A the type of the created [[SharedCache]]
 */
trait SharedCacheFactory[A <: SharedCache] {

    val targetClass: Class[A]

    /**
     * Creates a Shared Cache instance
     * @param channel the channel used by the shared cache
     * @return an instance of [[A]]
     * @see [[CachePacketChannel]]
     */
    def createNew(channel: CachePacketChannel): A

    final def factory: this.type = this //for Java users

}

object SharedCacheFactory {
    implicit def lambdaToFactory[A <: SharedCache](target: Class[_])(f: CachePacketChannel => A): SharedCacheFactory[A] = {
        new SharedCacheFactory[A] {
            override val targetClass: Class[A] = target.asInstanceOf[Class[A]]

            override def createNew(channel: CachePacketChannel): A = f(channel)
        }
    }
}