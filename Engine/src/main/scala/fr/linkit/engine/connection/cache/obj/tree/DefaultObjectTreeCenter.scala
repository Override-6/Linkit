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

package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.SynchronizedObjectCenter
import fr.linkit.api.connection.cache.obj.tree.{ObjectTreeCenter, SyncNode, SynchronizedObjectTree}
import fr.linkit.engine.connection.cache.obj.{CacheRepoContent, DefaultSynchronizedObjectCenter}
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter.ObjectTreeProfile
import fr.linkit.engine.connection.cache.obj.generation.SyncObjectInstantiationHelper

import scala.collection.mutable

class DefaultObjectTreeCenter[A <: AnyRef](center: SynchronizedObjectCenter[A]) extends ObjectTreeCenter[A] {

    private val trees = new mutable.HashMap[Int, DefaultSynchronizedObjectTree[A]]

    def addTree(id: Int, tree: DefaultSynchronizedObjectTree[A]): Unit = {
        if (trees.contains(id))
            throw new SynchronizedObjectException(s"A tree of id '$id' already exists.")
        if (tree.center ne center)
            throw new SynchronizedObjectException("Attempted to attach a tree that comes from another center of this center.")
        trees.put(id, tree)
    }

    def findTreeInternal(id: Int): Option[DefaultSynchronizedObjectTree[A]] = {
        trees.get(id)
    }

    override def findTree(id: Int): Option[SynchronizedObjectTree[A]] = {
        trees.get(id)
    }

    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(tree: SynchronizedObjectTree[_ <: A]): ObjectTreeProfile[A] = {
            val node = tree.rootNode
            val syncObject = node.synchronizedObject
            val (detached, subWrappers) = SyncObjectInstantiationHelper.detachedWrapperClone(syncObject)
            val subWrappersInfo = subWrappers.map(pair => (pair._1, pair._2.getNodeInfo))
            ObjectTreeProfile[A](tree.id, detached, node.ownerID, subWrappersInfo)
        }

        val array = trees.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

}
