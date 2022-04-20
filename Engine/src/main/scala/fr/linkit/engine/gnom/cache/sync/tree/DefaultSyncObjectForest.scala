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

import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.cache.sync.{SyncObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.reference.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.tree.node.{MutableSyncNode, UnknownObjectSyncNode}
import fr.linkit.engine.gnom.cache.sync.{CacheRepoContent, InternalSynchronizedObjectCache}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownObject

import scala.collection.mutable

class DefaultSyncObjectForest[A <: AnyRef](center: InternalSynchronizedObjectCache[A], omc: ObjectManagementChannel)
    extends AbstractNetworkPresenceHandler[SyncObjectReference](omc)
        with InitialisableNetworkObjectLinker[SyncObjectReference] with SynchronizedObjectForest[A] {

    private val trees        = new mutable.HashMap[Int, DefaultSynchronizedObjectTree[A]]
    private val unknownTrees = new mutable.HashMap[Int, UnknownTree]()

    /*
    * used to store objects whose synchronized version must have their bound identifier.
    * */
    private val linkedOrigins = mutable.HashMap.empty[AnyRef, SyncObjectReference]

    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[SyncObjectReference]

    override def findTree(id: Int): Option[SynchronizedObjectTree[A]] = {
        trees.get(id)
    }

    override def registerReference(ref: SyncObjectReference): Unit = {
        super.registerReference(ref)
    }

    override def unregisterReference(ref: SyncObjectReference): Unit = {
        super.unregisterReference(ref)
    }

    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(tree: SynchronizedObjectTree[A]): ObjectTreeProfile[A] = {
            val node       = tree.rootNode
            val syncObject = node.synchronizedObject
            ObjectTreeProfile[A](tree.id, syncObject, node.ownerID, tree.contractFactory.data)
        }

        val array = trees.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }

    override def findObject(location: SyncObjectReference): Option[NetworkObject[SyncObjectReference]] = {
        if (location.cacheID != center.cacheID || location.family != center.family)
            return None
        val path = location.nodePath
        trees.get(path.head)
            .flatMap(_.findNode(path).flatMap((x: ObjectNode[_]) => x match {
                case node: ObjectSyncNode[_] => Some(node.synchronizedObject)
                case _                       => None
            }))
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = handleBundle(bundle)

    override def initializeObject(obj: NetworkObject[_ <: SyncObjectReference]): Unit = {
        obj match {
            case syncObj: A with SynchronizedObject[A] => initializeSyncObject(syncObj)
            case _                                     => throwUnknownObject(obj)
        }
    }

    def linkWithReference(obj: AnyRef, ref: SyncObjectReference): Unit = {
        linkedOrigins(obj) = ref
    }

    def removeLinkedReference(obj: AnyRef): Option[SyncObjectReference] = linkedOrigins.remove(obj)

    def isObjectLinked(obj: AnyRef): Boolean = linkedOrigins.contains(obj)

    def isRegisteredAsUnknown(id: Int): Boolean = unknownTrees.contains(id)

    def transferUnknownTree(id: Int): Unit = {
        if (!trees.contains(id))
            throw new IllegalStateException(s"Can not transfer unknown tree with id $id: a tree with the same id must be created before transfering all UnknownTree objects into its 'Known' tree ")
        val tree = unknownTrees.remove(id).get
        tree.foreach { obj =>
            if (!obj.isInitialized)
                initializeSyncObject(obj)
        }
    }

    private[tree] def findMatchingNode(nonSyncNode: AnyRef): Option[ObjectSyncNode[_]] = {
        for (tree <- trees.values) {
            val opt = tree.findMatchingSyncNode(nonSyncNode)
            if (opt.isDefined)
                return opt
        }
        None
    }

    private def initializeSyncObject(syncObj: SynchronizedObject[_]): Unit = {
        val reference = syncObj.reference
        if (syncObj.isInitialized)
            return
        val path    = reference.nodePath
        val treeID  = path.head
        val treeOpt = findTreeInternal(treeID)
        if (treeOpt.isEmpty) {
            val tree = unknownTrees.getOrElseUpdate(treeID, new UnknownTree(treeID))
            tree.addUninitializedSyncObject(syncObj)
            return
        }
        val tree       = treeOpt.get
        val nodeOpt    = tree.findNode[AnyRef](path)
        val parentPath = path.dropRight(1)

        def castedSync[X <: AnyRef]: X with SynchronizedObject[X] = syncObj.asInstanceOf[X with SynchronizedObject[X]]

        if (nodeOpt.isEmpty) {
            tree.registerSynchronizedObject(parentPath, path.last, castedSync, reference.ownerID, None).synchronizedObject
        } else {
            nodeOpt.get match {
                case node: ObjectSyncNode[_]     =>
                    if (node.synchronizedObject ne syncObj)
                        throw new UnsupportedOperationException(s"Synchronized object already exists at $reference")
                case node: UnknownObjectSyncNode =>
                    val parent = node.parent.asInstanceOf[MutableSyncNode[AnyRef]]
                    val data   = center.newObjectData[A](parent, node.id, castedSync, None, reference.ownerID)
                    node.setAsKnownObjectNode(data)
            }
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
