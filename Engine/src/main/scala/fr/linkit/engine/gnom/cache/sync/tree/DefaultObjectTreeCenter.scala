/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.tree.{SyncNodeReference, SynchronizedObjectTree, SynchronizedObjectTreeStore}
import fr.linkit.api.gnom.cache.sync.{SynchronizedObject, SynchronizedObjectCache}
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.engine.gnom.cache.sync.CacheRepoContent
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCenter.ObjectTreeProfile
import fr.linkit.engine.gnom.reference.AbstractNetworkObjectLinker

import scala.collection.mutable

class DefaultObjectTreeCenter[A <: AnyRef](center: SynchronizedObjectCache[A], omc: ObjectManagementChannel)
        extends AbstractNetworkObjectLinker[SynchronizedObject[_], SyncNodeReference](omc) with SynchronizedObjectTreeStore[A] {

    private val trees = new mutable.HashMap[Int, DefaultSynchronizedObjectTree[A]]

    override def findTree(id: Int): Option[SynchronizedObjectTree[A]] = {
        trees.get(id)
    }

    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(tree: SynchronizedObjectTree[A]): ObjectTreeProfile[A] = {
            val node       = tree.rootNode
            val syncObject = node.synchronizedObject
            ObjectTreeProfile[A](tree.id, syncObject, node.ownerID)
        }

        val array = trees.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

    override def isPresent(location: SyncNodeReference): Boolean = {
        location.cacheID == center.cacheID && location.cacheFamily == center.family && {
            val path = location.nodePath
            trees.get(path.head).exists(_.findNode(path).isDefined)
        }
    }

    def addTree(id: Int, tree: DefaultSynchronizedObjectTree[A]): Unit = {
        if (trees.contains(id))
            throw new SynchronizedObjectException(s"A tree of id '$id' already exists.")
        if (tree.dataFactory ne center)
            throw new SynchronizedObjectException("Attempted to attach a tree that comes from an unknown cache.")
        trees.put(id, tree)
    }

    def findTreeInternal(id: Int): Option[DefaultSynchronizedObjectTree[A]] = {
        trees.get(id)
    }



/*
    override def findLocation(obj: SynchronizedObject[_]): Option[SyncNodeLocation] = {
        Some(obj.getLocation)
    }

    override def findObject(location: SyncNodeLocation): Option[SynchronizedObject[_]] = {
        if (location.cacheFamily == center.family && location.cacheID == center.cacheID) {
            val path = location.nodePath
            return trees.get(path.head).flatMap(_.findNode[AnyRef](path).map(_.synchronizedObject))
        }
        None
    }*/

    override def findObjectLocation(coordsOrigin: PacketCoordinates, ref: SynchronizedObject[_]): Option[SyncNodeReference] = {
        //The ref origin does not change its location
        Some(ref.getLocation)
    }

    override def findObjectLocation(ref: SynchronizedObject[_]): Option[SyncNodeReference] = {
        Some(ref.getLocation)
    }

    override def findObject(coordsOrigin: PacketCoordinates, location: SyncNodeReference): Option[SynchronizedObject[_]] = ???
}
