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

import fr.linkit.api.gnom.cache.SharedCacheReference
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.cache.sync.{ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.reference.linker.InitialisableNetworkObjectLinker
import fr.linkit.api.gnom.reference.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache.ObjectTreeProfile
import fr.linkit.engine.gnom.cache.sync.tree.node.{MutableNode, SyncNodeDataRequest, UnknownObjectSyncNode}
import fr.linkit.engine.gnom.cache.sync.{CacheRepoContent, InternalSynchronizedObjectCache}
import fr.linkit.engine.gnom.reference.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.reference.NOLUtils.throwUnknownObject

import scala.collection.mutable

class DefaultSyncObjectForest[A <: AnyRef](center: InternalSynchronizedObjectCache[A],
                                           cachePresenceHandler: NetworkPresenceHandler[SharedCacheReference],
                                           omc: ObjectManagementChannel)
        extends AbstractNetworkPresenceHandler[ConnectedObjectReference](cachePresenceHandler, omc)
                with InitialisableNetworkObjectLinker[ConnectedObjectReference] with SynchronizedObjectForest[A] {
    
    private val trees        = new mutable.HashMap[Int, DefaultConnectedObjectTree[A]]
    private val unknownTrees = new mutable.HashMap[Int, UnknownTree]()
    
    private[tree] val cacheOwnerID: String = center.ownerID
    
    /*
    * used to store objects whose synchronized version of keys already have bound references.
    * */
    private val linkedOrigins = mutable.HashMap.empty[AnyRef, ConnectedObjectReference]
    
    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[ConnectedObjectReference]
    
    override def findTree(id: Int): Option[ConnectedObjectTree[A]] = {
        trees.get(id)
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
    
    override def findObject(location: ConnectedObjectReference): Option[NetworkObject[ConnectedObjectReference]] = {
        if (location.cacheID != center.cacheID || location.family != center.family)
            return None
        val path = location.nodePath
        trees.get(path.head)
                .flatMap(_.findNode(path).map((_: MutableNode[_]).obj))
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
        AppLoggers.SyncObj.trace(s"Initialising synchronized object at $reference.")
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
    
    private[sync] def addTree(id: Int, tree: DefaultConnectedObjectTree[A]): Unit = {
        if (trees.contains(id))
            throw new SynchronizedObjectException(s"A tree of id '$id' already exists.")
        if (tree.dataFactory ne center)
            throw new SynchronizedObjectException("Attempted to attach a tree that comes from an unknown cache.")
        AppLoggers.SyncObj.debug(s"New tree added ($tree) in forest of cache ${center.reference}")
        registerReference(tree.rootNode.reference)
        trees.put(id, tree)
    }
    
    private[sync] def findTreeInternal(id: Int): Option[DefaultConnectedObjectTree[A]] = {
        trees.get(id)
    }
}
