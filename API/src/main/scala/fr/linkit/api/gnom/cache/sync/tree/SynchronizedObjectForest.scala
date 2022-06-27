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

package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.CacheContent

/**
 * Used by [[fr.linkit.api.gnom.cache.sync.ConnectedObjectCache]] to store its trees
 * @tparam A the type of the tree's root object (it's the same type for SynchronizedObjectCenter[A])
 */
trait SynchronizedObjectForest[A <: AnyRef] {

    /**
     * Finds a tree from its id. A tree identifier is the identifier of the root object.
     * @param id the tree's identifier.
     * @return Some(SynchronizedObjectTree[A]) if the tree is found, None instead.
     */
    def findTree(id: Int): Option[ConnectedObjectTree[A]]

    /**
     * Return the content of all stored trees.
     * The content of a tree is simply the detached root object (see [[fr.linkit.api.gnom.cache.sync.SynchronizedObject#detachedClone]])
     * @return
     */
    def snapshotContent: CacheContent
}
