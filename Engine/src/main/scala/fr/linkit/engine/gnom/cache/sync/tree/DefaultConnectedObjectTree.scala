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

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectContractFactory
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.contract.{SyncLevel, SyncObjectFieldManipulation}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceInstantiator
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.cache.sync.instantiation.{ContentSwitcher, MirroringInstanceCreator}
import fr.linkit.engine.gnom.cache.sync.invokation.UsageConnectedObjectContext
import fr.linkit.engine.gnom.cache.sync.tree.node._

import java.util.concurrent.ThreadLocalRandom
import scala.annotation.switch
import scala.util.Try

final class DefaultConnectedObjectTree[A <: AnyRef] private(currentIdentifier: String,
                                                            network: Network,
                                                            private[sync] val forest: DefaultSyncObjectForest[A],
                                                            val instantiator: SyncInstanceInstantiator,
                                                            val dataFactory: SyncNodeDataFactory,
                                                            override val id: Int,
                                                            override val contractFactory: ObjectContractFactory) extends ConnectedObjectTree[A] with ObjectConnector {
    
    private var root: RootObjectNodeImpl[A] = _
    
    def this(currentIdentifier: String,
             network: Network,
             center: DefaultSyncObjectForest[A],
             id: Int,
             instantiator: SyncInstanceInstantiator,
             dataFactory: SyncNodeDataFactory,
             behaviorTree: ObjectContractFactory)(rootSupplier: DefaultConnectedObjectTree[A] => RootObjectNodeImpl[A]) = {
        this(currentIdentifier, network, center, instantiator, dataFactory, id, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalObjectNodeException("Root node's tree != this")
        if (root.id != id)
            throw new IllegalObjectNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
        scanSyncObjectFields(root, currentIdentifier, root.obj)
    }
    
    def getRoot: RootObjectNodeImpl[A] = root
    
    override def rootNode: ObjectSyncNode[A] = root
    
    override def findNode[B <: AnyRef](path: Array[Int]): Option[MutableNode[B]] = {
        checkPath(path)
        findNode0[B](path)
    }
    
    private[tree] def findMatchingSyncNode(nonSyncObject: AnyRef): Option[ObjectSyncNode[_ <: AnyRef]] = {
        nonSyncObject match {
            case sync: SynchronizedObject[_] => Some(sync.getNode)
            case _                           =>
                root.getMatchingSyncNode(nonSyncObject) match {
                    case node: InternalObjectSyncNode[_] => Some(node)
                    case _                               => None
                }
        }
    }
    
    private def checkPath(path: Array[Int]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }
    
    override def insertObject[B <: AnyRef](parent: ConnectedObjectNode[_], source: AnyRef, ownerID: String, insertionKind: SyncLevel): ConnectedObjectNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owned by this tree's cache.")
        insertObject[B](parent.nodePath, source, ownerID, insertionKind)
    }
    
    override def insertObject[B <: AnyRef](parentPath: Array[Int], obj: AnyRef, ownerID: String, insertionKind: SyncLevel, idHint: Int = ThreadLocalRandom.current().nextInt()): ConnectedObjectNode[B] = {
        insertObject0(parentPath, obj, ownerID, insertionKind, idHint)
    }
    
    override def createConnectedObj(parentRef: ConnectedObjectReference, idHint: Int = ThreadLocalRandom.current().nextInt())(obj: Any, insertionKind: SyncLevel): ConnectedObject[AnyRef] = {
        val currentPath = parentRef.nodePath
        insertObject0(currentPath, obj.asInstanceOf[AnyRef], currentIdentifier, insertionKind, idHint).obj
    }
    
    private def insertObject0[B <: AnyRef](parentPath: Array[Int], obj: AnyRef, ownerID: String, insertionKind: SyncLevel, idHint: Int): ConnectedObjectNode[B] = {
        val parentNode = findNode[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        (insertionKind: @switch) match {
            case NotRegistered                       => throw new IllegalArgumentException("insertionKind = NotRegistered.")
            case ChippedOnly | Synchronized | Mirror =>
                val id = forest
                        .removeLinkedReference(obj)
                        .map(_.nodePath.last)
                        .getOrElse(idHint)
                getOrGenConnectedObject[B](parentNode, id, obj.asInstanceOf[B], insertionKind)(ownerID)
        }
    }
    
    override def createMirroredObject[B <: AnyRef](parentPath: Array[Int], classDef: SyncClassDef, ownerID: String, idHint: Int): ConnectedObjectNode[B] = {
        val parentNode                     = findNode[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        val id                             = idHint
        val syncObject                     = instantiator.newSynchronizedInstance[B](new MirroringInstanceCreator[B](classDef))
        val result: ConnectedObjectNode[B] = initSynchronizedObject[B](parentNode, id, syncObject, None, ownerID, true)
        result
    }
    
    private def findNode0[B <: AnyRef](path: Array[Int]): Option[MutableNode[B]] = {
        if (path.isEmpty || path.head != root.id)
            return None
        var ch: MutableNode[_] = root
        for (childID <- path.drop(1)) {
            val opt = ch.getChild[B](childID)
            if (opt.isEmpty)
                return None
            ch = opt.get
        }
        Option(ch.asInstanceOf[MutableNode[B]])
    }
    
    private def getOrGenConnectedObject[B <: AnyRef](parent: MutableNode[_ <: AnyRef], id: Int,
                                                     source: B, level: SyncLevel)(ownerID: String): ConnectedObjectNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")
        if (level == NotRegistered)
            throw new IllegalArgumentException("insertionKind = NotRegistered.")
        if (source == null)
            throw new NullPointerException("source object is null.")
        if (source.isInstanceOf[ConnectedObject[_]])
            throw new CanNotSynchronizeException("This object is already a connected object.")
        
        forest.findMatchingNode(source) match {
            case Some(value: ObjectSyncNode[B])    =>
                if (level != Synchronized && level != Mirror)
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a SynchronizedObject node")
                value
            case Some(value: ChippedObjectNode[B]) =>
                if (level != ChippedOnly && level != Mirror)
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a ChippedObject node")
                value
            case None                              =>
                level match {
                    case ChippedOnly  =>
                        newChippedObject(parent, id, source)
                    case Synchronized | Mirror =>
                        val syncObject = instantiator.newSynchronizedInstance[B](new ContentSwitcher[B](source))
                        initSynchronizedObject[B](parent, id, syncObject, Some(source), ownerID, level == Mirror)
                }
            
        }
    }
    
    private def newChippedObject[B <: AnyRef](parent: MutableNode[_ <: AnyRef], id: Int, chipped: B): ChippedObjectNode[B] = {
        //if (ownerID != currentIdentifier)
        //    throw new IllegalConnectedObjectRegistration("Attempted to create a chipped object that is not owned by the current engine. Chipped Objects can only exists on their origin engines.")
        val adapter = new ChippedObjectAdapter[B](chipped)
        initChippedObject(parent, id, adapter)
    }
    
    private def initChippedObject[B <: AnyRef](parent: MutableNode[_ <: AnyRef], id: Int, adapter: ChippedObjectAdapter[B]): ChippedObjectNode[B] = {
        val data = dataFactory.newNodeData(new ChippedObjectNodeDataRequest[B](parent, id, adapter, currentIdentifier))
        val node = new ChippedObjectNodeImpl[B](data)
        parent.addChild(node)
        adapter.initialize(node)
        node
    }
    
    private def initSynchronizedObject[B <: AnyRef](parent: MutableNode[_], id: Int,
                                                    syncObject: B with SynchronizedObject[B], origin: Option[B],
                                                    ownerID: String, isMirroring: Boolean): ObjectSyncNodeImpl[B] = {
        if (syncObject.isInitialized)
            throw new ConnectedObjectAlreadyInitialisedException(s"Could not register synchronized object '${syncObject.getClass.getName}' : Object already initialized.")
        
        val level = if (isMirroring) Mirror else Synchronized
        val data  = dataFactory.newNodeData(new SyncNodeDataRequest[B](parent.asInstanceOf[MutableNode[AnyRef]], id, syncObject, origin, ownerID, level))
        val node  = new ObjectSyncNodeImpl[B](data)
        forest.registerReference(node.reference)
        parent.addChild(node)
        
        scanSyncObjectFields(node, ownerID, syncObject)
        node
    }
    
    @inline
    private def scanSyncObjectFields[B <: AnyRef](node: ObjectSyncNodeImpl[B], ownerID: String, syncObject: B with SynchronizedObject[B]): Unit = {
        val isCurrentOwner = ownerID == currentIdentifier
        val engine0        = if (!isCurrentOwner) Try(network.findEngine(ownerID).get).getOrElse(null) else null
        val manipulation   = new SyncObjectFieldManipulation {
            override val engine: Engine = engine0
            
            override def findConnectedVersion(origin: Any): Option[ConnectedObject[AnyRef]] = {
                cast(findMatchingSyncNode(cast(origin)).map(_.obj))
            }
            
            override def initObject(sync: ConnectedObject[AnyRef]): Unit = {
                val id = sync.reference.nodePath.last
                sync match {
                    case sync: SynchronizedObject[AnyRef]         =>
                        initSynchronizedObject[AnyRef](node, id, sync, Some(sync), ownerID, false)
                    case chippedObj: ChippedObjectAdapter[AnyRef] =>
                        newChippedObject[AnyRef](node, id, chippedObj)
                }
            }
            
            override def createConnectedObject(obj: AnyRef, kind: SyncLevel): ConnectedObject[AnyRef] = {
                val id = forest.removeLinkedReference(obj)
                        .map(_.nodePath.last)
                        .getOrElse(ThreadLocalRandom.current().nextInt())
                getOrGenConnectedObject(node, id, obj, kind)(ownerID).obj
            }
        }
        node.contract.applyFieldsContracts(syncObject, manipulation)
    }
    
    private def cast[X](y: Any): X = y.asInstanceOf[X]
    
    private def createUnknownObjectNode(path: Array[Int]): MutableNode[AnyRef] = {
        val parent = getParent(path.dropRight(1))
        val data   = dataFactory.newNodeData(new NormalNodeDataRequest[AnyRef](parent, path, null))
        val node   = new UnknownObjectSyncNode(data)
        parent.addChild(node)
        node
    }
    
    private def getParent[B <: AnyRef](parentPath: Array[Int]): MutableNode[B] = {
        findNode[B](parentPath).getOrElse {
            createUnknownObjectNode(parentPath).asInstanceOf[MutableNode[B]]
        }
    }
    
    private[tree] def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int,
                                                              syncObject: B with SynchronizedObject[B], ownerID: String,
                                                              origin: Option[B]): ObjectSyncNode[B] = {
        val wrapperNode = getParent[B](parentPath)
        initSynchronizedObject[B](wrapperNode, id, syncObject, origin, ownerID, syncObject.isMirrored)
    }
    
}