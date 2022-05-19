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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.SharedCacheReference

import java.util

/**
 * All the information that allows to retrieve the synchronized object node.
 * @param cacheFamily the cache family of the object cache's manager.
 * @param cacheID the object cache identifier
 * @param ownerID the owner of the object (the engine's identifier that created the object)
 * @param nodePath the path of the object's node in its [[fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectTree]]
 */
class ConnectedObjectReference(family: String,
                               cacheID: Int,
                               val ownerID: String,
                               val nodePath: Array[Int]) extends SharedCacheReference(family, cacheID) {

    override def asSuper: Option[SharedCacheReference] = Some(new SharedCacheReference(family, cacheID))

    override def toString: String = super.toString + s"/~${nodePath.mkString("/")}"

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(family, cacheID, ownerID, nodePath))

    override def equals(obj: Any): Boolean = obj match {
        case ref: ConnectedObjectReference => ref.family == family &&
                ref.cacheID == cacheID &&
                ref.ownerID == ownerID &&
                (ref.nodePath sameElements nodePath)
        case _                             => false
    }

}

object ConnectedObjectReference {

    def apply(family: String, cacheID: Int, owner: String, nodePath: Array[Int]): ConnectedObjectReference = new ConnectedObjectReference(family, cacheID, owner, nodePath)
}