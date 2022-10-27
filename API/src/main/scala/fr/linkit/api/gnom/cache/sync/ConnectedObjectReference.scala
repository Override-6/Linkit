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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.referencing.NamedIdentifier

import java.util

/**
 * All the information that allows to retrieve the synchronized object node.
 * @param cacheFamily the cache family of the object cache's manager.
 * @param cacheID the object cache identifier
 * @param ownerID the owner of the object (the engine's name that created the object)
 *                NOTE: The value of this field have no influence on the reference location linking, it's just informal.
 * @param nodePath the path of the object's node in its [[fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectTree]]
 */
class ConnectedObjectReference(family: String,
                               cacheID: Int,
                               val ownerID: String,
                               val nodePath: Array[NamedIdentifier]) extends SharedCacheReference(family, cacheID) {

    override def asSuper: Option[SharedCacheReference] = Some(new SharedCacheReference(family, cacheID))

    override def toString: String = super.toString + s"/~${nodePath.mkString("/")}"

    override def hashCode(): Int = util.Arrays.deepHashCode(Array(family, cacheID, nodePath))

    override def equals(obj: Any): Boolean = obj match {
        case ref: ConnectedObjectReference => ref.family == family &&
                ref.cacheID == cacheID &&
                //ref.ownerID == ownerID &&
                (ref.nodePath sameElements nodePath)
        case _                             => false
    }

}

object ConnectedObjectReference {

    def apply(family: String, cacheID: Int, owner: String, nodePath: Array[NamedIdentifier]): ConnectedObjectReference = new ConnectedObjectReference(family, cacheID, owner, nodePath)
}