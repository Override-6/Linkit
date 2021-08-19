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

package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.CacheContent

/**
 * Used by [[fr.linkit.api.connection.cache.obj.SynchronizedObjectCenter]] to store its trees
 * @tparam A the type of the tree's root object (it's the same type for SynchronizedObjectCenter[A])
 */
trait SynchronizedObjectTreeStore[A <: AnyRef] {

    /**
     * Finds a tree from its id. A tree identifier is the identifier of the root object.
     * @param id the tree's identifier.
     * @return Some(SynchronizedObjectTree[A]) if the tree is found, None instead.
     */
    def findTree(id: Int): Option[SynchronizedObjectTree[A]]

    /**
     * Return the content of all stored trees.
     * The content of a tree is simply the detached root object (see [[fr.linkit.api.connection.cache.obj.SynchronizedObject#detachedClone]])
     * @return
     */
    def snapshotContent: CacheContent
}
