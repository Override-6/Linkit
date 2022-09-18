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

package fr.linkit.engine.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.cache.sync.{ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.referencing.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.gnom.referencing.{NamedIdentifier, NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.tree.node.{MutableNode, SyncNodeDataRequest, UnknownObjectSyncNode}
import fr.linkit.engine.gnom.cache.sync.{CacheRepoContent, InternalConnectedObjectCache}
import fr.linkit.engine.gnom.referencing.NOLUtils.throwUnknownObject
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler

import scala.collection.mutable

class DefaultSyncObjectForest[A <: AnyRef](center: InternalConnectedObjectCache[A],
                                           cachePresenceHandler: NetworkPresenceHandler[SharedCacheReference],
                                           omc: ObjectManagementChannel)
        extends AbstractNetworkPresenceHandler[ConnectedObjectReference](cachePresenceHandler, omc)
                with InitialisableNetworkObjectLinker[ConnectedObjectReference] with SynchronizedObjectForest[A] {
    
    private val trees        = new mutable.HashMap[NamedIdentifier, DefaultConnectedObjectTree[A]]
    private val unknownTrees = new mutable.HashMap[NamedIdentifier, UnknownTree]()
    
    /*
    * used to store objects whose synchronized version of keys already have bounded references.
    * */
    private val linkedOrigins = mutable.HashMap.empty[AnyRef, ConnectedObjectReference]

    override def findTree(id: NamedIdentifier): Option[ConnectedObjectTree[A]] = findTreeInternal(id)
    
    def findTreeLocal(id: NamedIdentifier): Option[ConnectedObjectTree[A]] = trees.get(id)
    
    private[sync] def findTreeInternal(id: NamedIdentifier): Option[DefaultConnectedObjectTree[A]] = {
        trees.get(id).orElse {
            center.requestTree(id)
            trees.get(id)
        }
    }
    
    def putUnknownTree(id: NamedIdentifier): Unit = {
        if (unknownTrees.contains(id))
            return
        if (trees.contains(id))
            throw new IllegalStateException(s"Tree $id is known !")
        unknownTrees.put(id, new UnknownTree(id))
    }
    
    override def registerReference(ref: ConnectedObjectReference): Unit = {
        super.registerReference(ref)
    }
    
    override def unregisterReference(ref: ConnectedObjectReference): Unit = {
        super.unregisterReference(ref)
    }
    
    override def snapshotContent: CacheRepoContent[A] = {
        def toProfile(tree: ConnectedObjectTree[A]): ObjectTreeProfile[A] = {
            val node       = tree.rootNode
            val syncObject = node.obj
            val mirror     = syncObject.isMirroring || syncObject.isMirrored
            ObjectTreeProfile[A](tree.id, syncObject, node.ownerID, mirror, tree.contractFactory.data)
        }
        
        val array = trees.values.map(toProfile).toArray
        new CacheRepoContent[A](array)
    }
    
    override def findObject(reference: ConnectedObjectReference): Option[NetworkObject[ConnectedObjectReference]] = {
        if (reference.cacheID != center.cacheID || reference.family != center.family)
            return None
        val path = reference.nodePath
        if (unknownTrees.contains(path.head))
            return None
        val node = findTreeInternal(path.head)
        node.flatMap(_.findNode(path).map((_: MutableNode[_]).obj))
    }
    
    override def initializeObject(obj: NetworkObject[_ <: ConnectedObjectReference]): Unit = {
        obj match {
            case syncObj: A with SynchronizedObject[A] => initializeSyncObject(syncObj)
            case _                                     => throwUnknownObject(obj)
        }
    }
    
    def linkWithReference(obj: AnyRef, ref: ConnectedObjectReference): Unit = {
        linkedOrigins(obj) = ref
    }
    
    def removeLinkedReference(obj: AnyRef): Option[ConnectedObjectReference] = linkedOrigins.remove(obj)
    
    def isObjectLinked(obj: AnyRef): Boolean = linkedOrigins.contains(obj)
    
    def isRegisteredAsUnknown(id: NamedIdentifier): Boolean = unknownTrees.contains(id)
    
    def transferUnknownTree(id: NamedIdentifier): Unit = {
        if (findTreeInternal(id).isEmpty)
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
        AppLoggers.ConnObj.trace(s"Initialising connected object at $reference.")
        val path    = reference.nodePath
        val treeID  = path.head
        val treeOpt = trees.get(treeID)
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
            tree.registerSynchronizedObject(parentPath, path.last, castedSync, reference.ownerID, None).obj
        } else {
            nodeOpt.get match {
                case node: ObjectSyncNode[_]     =>
                    if (node.obj ne syncObj)
                        throw new UnsupportedOperationException(s"Synchronized object already exists at $reference")
                case node: UnknownObjectSyncNode =>
                    val parent = node.parent.asInstanceOf[MutableNode[AnyRef]]
                    val level  = if (syncObj.isMirrored) SyncLevel.Mirror else SyncLevel.Synchronized
                    val data   = center.newNodeData(new SyncNodeDataRequest[A](parent, node.id, castedSync, None, reference.ownerID, level))
                    node.setAsKnownObjectNode(data)
            }
        }
    }
    
    private[sync] def addTree(id: NamedIdentifier, tree: DefaultConnectedObjectTree[A]): Unit = {
        if (trees.contains(id))
            throw new SynchronizedObjectException(s"A tree with id '$id' already exists.")
        if (tree.dataFactory ne center)
            throw new SynchronizedObjectException("Attempted to attach a tree that comes from an unknown cache.")
        AppLoggers.ConnObj.debug(s"New tree added ${tree.rootNode.reference}.")
        registerReference(tree.rootNode.reference)
        trees.put(id, tree)
    }
}
