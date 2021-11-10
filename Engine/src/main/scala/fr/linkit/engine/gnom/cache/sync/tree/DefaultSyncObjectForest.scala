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

package fr.linkit.engine.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference, SynchronizedObjectForest, SynchronizedObjectTree}
import fr.linkit.api.gnom.cache.sync.{SynchronizedObject, SynchronizedObjectCache}
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.gnom.reference.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.{CacheRepoContent, InternalSynchronizedObjectCache}
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownObject

import scala.collection.mutable

class DefaultSyncObjectForest[A <: AnyRef](center: InternalSynchronizedObjectCache[A], omc: ObjectManagementChannel)
    extends AbstractNetworkPresenceHandler[SyncObjectReference](omc)
        with InitialisableNetworkObjectLinker[SyncObjectReference] with SynchronizedObjectForest[A] {

    private val trees = new mutable.HashMap[Int, DefaultSynchronizedObjectTree[A]]

    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[SyncObjectReference]

    def findNode(nonSyncNode: AnyRef): Option[SyncNode[_]] = {
        for (tree <- trees.values) {
            val opt = tree.findMatchingSyncNode(nonSyncNode)
            if (opt.isDefined)
                return opt
        }
        None
    }

    override def findTree(id: Int): Option[SynchronizedObjectTree[A]] = {
        trees.get(id)
    }

    override def registerReference(ref: SyncObjectReference): Unit = super.registerReference(ref)

    override def unregisterReference(ref: SyncObjectReference): Unit = super.unregisterReference(ref)

    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(tree: SynchronizedObjectTree[A]): ObjectTreeProfile[A] = {
            val node       = tree.rootNode
            val syncObject = node.synchronizedObject
            ObjectTreeProfile[A](tree.id, syncObject, node.ownerID)
        }

        val array = trees.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

    override def findObject(location: SyncObjectReference): Option[NetworkObject[SyncObjectReference]] = {
        if (location.cacheID != center.cacheID || location.family != center.family)
            return None
        val path = location.nodePath
        trees.get(path.head)
            .flatMap(_.findNode(path)
                .map((_: SyncNode[_]).synchronizedObject))
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = handleBundle(bundle)

    override def initializeObject(obj: NetworkObject[_ <: SyncObjectReference]): Unit = {
        obj match {
            case syncObj: SynchronizedObject[A] => initializeSyncObject(syncObj)
            case _                              => throwUnknownObject(obj)
        }
    }

    private def initializeSyncObject(syncObj: SynchronizedObject[A]): Unit = {
        val reference = syncObj.reference
        val path      = reference.nodePath
        var treeOpt   = findTreeInternal(path.head)
        if (treeOpt.isEmpty) {
            if (path.length == 1) {
                center.makeTree(syncObj)
                treeOpt = findTreeInternal(path.head)
            } else {
                throw new NoSuchObjectTreeException(s"No Object Tree found of id ${path.head} for object at $reference.")
            }
        }
        val tree    = treeOpt.get
        val nodeOpt = tree.findNode(path)
        if (nodeOpt.isEmpty) {
            def castedSyncObject[X <: AnyRef](): X with SynchronizedObject[X] = syncObj.asInstanceOf[X with SynchronizedObject[X]]

            tree.registerSynchronizedObject(path.dropRight(1), path.last, castedSyncObject(), reference.owner, None).synchronizedObject
        } else if (nodeOpt.get.synchronizedObject ne syncObj) {
            throw new UnsupportedOperationException(s"Synchronized object already exists at $reference")
        }
    }

    private[sync] def addTree(id: Int, tree: DefaultSynchronizedObjectTree[A]): Unit = {
        if (trees.contains(id))
            throw new SynchronizedObjectException(s"A tree of id '$id' already exists.")
        if (tree.dataFactory ne center)
            throw new SynchronizedObjectException("Attempted to attach a tree that comes from an unknown cache.")
        registerReference(tree.rootNode.reference)
        trees.put(id, tree)
    }

    private[sync] def findTreeInternal(id: Int): Option[DefaultSynchronizedObjectTree[A]] = {
        trees.get(id)
    }
}
