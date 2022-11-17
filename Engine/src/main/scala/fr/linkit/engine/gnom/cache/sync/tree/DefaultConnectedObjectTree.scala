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

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectContractFactory
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.contract.{SyncLevel, SyncObjectFieldManipulation}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceInstantiator
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.tag.{Current, EngineSelector, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.cache.sync.instantiation.{ContentSwitcher, MirroringInstanceCreator}
import fr.linkit.engine.gnom.cache.sync.tree.node._

import java.util.concurrent.ThreadLocalRandom
import scala.annotation.switch
import scala.util.Try

final class DefaultConnectedObjectTree[A <: AnyRef] private(resolver                    : EngineSelector,
                                                            private[sync] val forest    : DefaultSyncObjectForest[A],
                                                            val instantiator            : SyncInstanceInstantiator,
                                                            val dataFactory             : NodeDataFactory,
                                                            override val id             : NamedIdentifier,
                                                            override val contractFactory: ObjectContractFactory) extends ConnectedObjectTree[A] with ObjectConnector {

    private var root: RootObjectNodeImpl[A] = _

    def this(resolver    : EngineSelector,
             center      : DefaultSyncObjectForest[A],
             id          : NamedIdentifier,
             instantiator: SyncInstanceInstantiator,
             dataFactory : NodeDataFactory,
             behaviorTree: ObjectContractFactory)(rootSupplier: DefaultConnectedObjectTree[A] => RootObjectNodeImpl[A]) = {
        this(resolver, center, instantiator, dataFactory, id, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalObjectNodeException("Root node's tree != this")
        if (root.id != id)
            throw new IllegalObjectNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
        scanSyncObjectFields(root, Current, root.obj)
    }

    def getRoot: RootObjectNodeImpl[A] = root

    override def rootNode: ObjectSyncNode[A] = root

    override def findNode[B <: AnyRef](path: Array[NamedIdentifier]): Option[MutableNode[B]] = {
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

    private def checkPath(path: Array[NamedIdentifier]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }

    override def insertObject[B <: AnyRef](parent       : ConnectedObjectNode[_],
                                           source       : AnyRef,
                                           ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                           insertionKind: SyncLevel): ConnectedObjectNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owned by this tree's cache.")
        insertObject[B](parent.nodePath, source, ownerID, insertionKind)
    }

    override def insertObject[B <: AnyRef](parentPath   : Array[NamedIdentifier],
                                           source       : AnyRef,
                                           ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                           insertionKind: SyncLevel): ConnectedObjectNode[B] = {
        insertObject(parentPath, source, ownerID, insertionKind, source match {
            case e: Engine => e.hashCode() //as Engines are handled specifically by the system, they need a hash-based id
            case _         => ThreadLocalRandom.current().nextInt()
        })
    }

    override def createConnectedObj(parentRef: ConnectedObjectReference)(obj: Any, kind: SyncLevel): ConnectedObject[AnyRef] = {
        createConnectedObj(parentRef, obj match {
            case e: Engine => e.hashCode() //as Engines are handled specifically by the system, they need a hash-based id
            case _         => ThreadLocalRandom.current().nextInt()
        })(obj, kind)
    }

    override def insertObject[B <: AnyRef](parentPath   : Array[NamedIdentifier],
                                           obj          : AnyRef,
                                           ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                           insertionKind: SyncLevel,
                                           idHint       : Int): ConnectedObjectNode[B] = {
        insertObject0(parentPath, obj, ownerID, insertionKind, idHint)
    }

    override def createConnectedObj(parentRef: ConnectedObjectReference,
                                    id       : Int)(obj: Any, insertionKind: SyncLevel): ConnectedObject[AnyRef] = {
        val currentPath = parentRef.nodePath
        insertObject0(currentPath, obj.asInstanceOf[AnyRef], Current, insertionKind, id).obj
    }

    private def getNode[B <: AnyRef](path: Array[NamedIdentifier]): MutableNode[B] = findNode[B](path).getOrElse {
        throw new IllegalArgumentException(s"Could not find parent path in this object tree (${path.mkString("/")}) (tree id == ${this.id}).")
    }

    private def insertObject0[B <: AnyRef](parentPath: Array[NamedIdentifier], obj: AnyRef, ownerID: UniqueTag with NetworkFriendlyEngineTag, insertionKind: SyncLevel, idHint: Int): ConnectedObjectNode[B] = {
        val parentNode = getNode(parentPath)
        (insertionKind: @switch) match {
            case NotRegistered                   => throw new IllegalArgumentException("insertionKind = NotRegistered.")
            case Chipped | Synchronized | Mirror =>
                val id = forest
                        .removeLinkedReference(obj)
                        .map(_.nodePath.last)
                        .getOrElse(NamedIdentifier(obj.getClass.getSimpleName, idHint))
                getOrGenConnectedObject[B](parentNode, id, obj.asInstanceOf[B], insertionKind)(ownerID)
        }
    }

    override def createMirroredObject[B <: AnyRef](parentPath: Array[NamedIdentifier], classDef: SyncClassDef, ownerID: UniqueTag with NetworkFriendlyEngineTag, id: Int): ConnectedObjectNode[B] = {
        val parentNode                     = getNode(parentPath)
        val syncObject                     = instantiator.newSynchronizedInstance[B](new MirroringInstanceCreator[B](classDef))
        val result: ConnectedObjectNode[B] = initSynchronizedObject[B](parentNode, id, syncObject, None, ownerID, true)
        result
    }

    private def findNode0[B <: AnyRef](path: Array[NamedIdentifier]): Option[MutableNode[B]] = {
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

    private def getOrGenConnectedObject[B <: AnyRef](parent: MutableNode[_ <: AnyRef],
                                                     id    : NamedIdentifier,
                                                     source: B, level: SyncLevel)(ownerID: UniqueTag with NetworkFriendlyEngineTag): ConnectedObjectNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")
        if (level == NotRegistered)
            throw new IllegalArgumentException("insertionKind = NotRegistered.")
        if (source == null)
            throw new NullPointerException("source object is null.")
        if (source.isInstanceOf[ConnectedObject[_]])
            throw new CannotConnectException("This object is already a connected object.")

        forest.findMatchingNode(source) match {
            case Some(value: ObjectSyncNode[B])    =>
                if (level != Synchronized && level != Mirror)
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a SynchronizedObject node")
                value
            case Some(value: ChippedObjectNode[B]) =>
                if (level != Chipped && level != Mirror)
                    throw new IllegalObjectNodeException(s"requested $level object node but the source object '$source' is bound to a ChippedObject node")
                value
            case None                              =>
                level match {
                    case Chipped               =>
                        newChippedObject(parent, id, source)
                    case Synchronized | Mirror =>
                        val syncObject = instantiator.newSynchronizedInstance[B](new ContentSwitcher[B](source))
                        initSynchronizedObject[B](parent, id, syncObject, Some(source), ownerID, level == Mirror)
                }

        }
    }

    private def newChippedObject[B <: AnyRef](parent : MutableNode[_ <: AnyRef],
                                              id     : NamedIdentifier,
                                              chipped: B): ChippedObjectNode[B] = {
        //if (ownerID != currentIdentifier)
        //    throw new IllegalConnectedObjectRegistration("Attempted to create a chipped object that is not owned by the current engine. Chipped Objects can only exists on their origin engines.")
        val adapter = new ChippedObjectAdapter[B](chipped)
        initChippedObject(parent, id, adapter)
    }

    private def initChippedObject[B <: AnyRef](parent : MutableNode[_ <: AnyRef],
                                               id     : NamedIdentifier,
                                               adapter: ChippedObjectAdapter[B]): ChippedObjectNode[B] = {
        val currentNT = resolver(Current).nameTag
        val data      = dataFactory.newNodeData(new ChippedObjectNodeDataRequest[B](parent, id, adapter, currentNT))
        val node      = new ChippedObjectNodeImpl[B](data)
        parent.addChild(node)
        adapter.initialize(node)
        node
    }

    private def initSynchronizedObject[B <: AnyRef](parent     : MutableNode[_],
                                                    id         : NamedIdentifier,
                                                    syncObject : B with SynchronizedObject[B],
                                                    origin     : Option[B],
                                                    ownerID    : UniqueTag with NetworkFriendlyEngineTag,
                                                    isMirroring: Boolean): ObjectSyncNodeImpl[B] = {
        if (syncObject.isInitialized)
            throw new ConnectedObjectAlreadyInitialisedException(s"Could not register synchronized object '${syncObject.getClass.getName}' : Object already initialized.")

        val level   = if (isMirroring) Mirror else Synchronized
        val ownerNT = resolver(ownerID).nameTag
        val data    = dataFactory.newNodeData(new SyncNodeDataRequest[B](parent.asInstanceOf[MutableNode[AnyRef]], id, syncObject, origin, ownerNT, level))
        val node    = new ObjectSyncNodeImpl[B](data)
        forest.registerReference(node.reference)
        parent.addChild(node)

        scanSyncObjectFields(node, ownerID, syncObject)
        node
    }

    @inline
    private def scanSyncObjectFields[B <: AnyRef](node      : ObjectSyncNodeImpl[B],
                                                  ownerID   : UniqueTag with NetworkFriendlyEngineTag,
                                                  syncObject: B with SynchronizedObject[B]): Unit = {
        val isCurrentOwner = resolver.getEngine(ownerID).exists(_.isServer)
        val engine0        = if (!isCurrentOwner) Try(resolver.getEngine(ownerID).get).getOrElse(null) else null
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
                        .getOrElse(NamedIdentifier(obj.getClass.getSimpleName, ThreadLocalRandom.current().nextInt()))
                getOrGenConnectedObject(node, id, obj, kind)(ownerID).obj
            }
        }
        node.contract.applyFieldsContracts(syncObject, manipulation)
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]

    private def createUnknownObjectNode(path: Array[NamedIdentifier]): MutableNode[AnyRef] = {
        val parent = getParent(path.dropRight(1))
        val data   = dataFactory.newNodeData(new NormalNodeDataRequest[AnyRef](parent, path, null))
        val node   = new UnknownObjectSyncNode(data)
        parent.addChild(node)
        node
    }

    private def getParent[B <: AnyRef](parentPath: Array[NamedIdentifier]): MutableNode[B] = {
        findNode[B](parentPath).getOrElse {
            createUnknownObjectNode(parentPath).asInstanceOf[MutableNode[B]]
        }
    }

    private[tree] def registerSynchronizedObject[B <: AnyRef](parentPath: Array[NamedIdentifier],
                                                              id        : NamedIdentifier,
                                                              syncObject: B with SynchronizedObject[B],
                                                              ownerID   : UniqueTag with NetworkFriendlyEngineTag,
                                                              origin    : Option[B]): ObjectSyncNode[B] = {
        val wrapperNode = getParent[B](parentPath)
        initSynchronizedObject[B](wrapperNode, id, syncObject, origin, ownerID, syncObject.isMirrored)
    }

}