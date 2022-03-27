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

import fr.linkit.api.gnom.cache.sync.contract.SyncObjectFieldManipulation
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectContractFactory
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceInstantiator
import fr.linkit.api.gnom.cache.sync.tree.{ObjectSyncNode, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.instantiation.ContentSwitcher
import fr.linkit.engine.gnom.cache.sync.tree.node._
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator
import fr.linkit.engine.internal.utils.ScalaUtils

import java.util.concurrent.ThreadLocalRandom
import scala.util.Try

final class DefaultSynchronizedObjectTree[A <: AnyRef] private(currentIdentifier: String,
                                                               network: Network,
                                                               forest: DefaultSyncObjectForest[A],
                                                               val instantiator: SyncInstanceInstantiator,
                                                               val dataFactory: SyncNodeDataFactory,
                                                               override val id: Int,
                                                               override val contractFactory: ObjectContractFactory) extends SynchronizedObjectTree[A] {

    private var root: RootObjectSyncNodeImpl[A] = _

    def this(currentIdentifier: String,
             network: Network,
             center: DefaultSyncObjectForest[A],
             id: Int,
             instantiator: SyncInstanceInstantiator,
             dataFactory: SyncNodeDataFactory,
             behaviorTree: ObjectContractFactory)(rootSupplier: DefaultSynchronizedObjectTree[A] => RootObjectSyncNodeImpl[A]) = {
        this(currentIdentifier, network, center, instantiator, dataFactory, id, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalWrapperNodeException("Root node's tree != this")
        if (root.id != id)
            throw new IllegalWrapperNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
        scanSyncObjectFields(root, currentIdentifier, root.synchronizedObject)
    }

    def getRoot: RootObjectSyncNodeImpl[A] = root

    override def rootNode: ObjectSyncNode[A] = root

    override def findNode[B <: AnyRef](path: Array[Int]): Option[MutableSyncNode[B]] = {
        checkPath(path)
        findNode0[B](path)
    }

    private[tree] def findMatchingSyncNode(nonSyncObject: AnyRef): Option[ObjectSyncNode[_ <: AnyRef]] = {
        nonSyncObject match {
            case sync: SynchronizedObject[_] => findNode0(sync.reference.nodePath)
            case _                           =>
                Option(root.getMatchingSyncNode(nonSyncObject))
        }
    }

    private def checkPath(path: Array[Int]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }

    override def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, source: B, ownerID: String): ObjectSyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owner by this tree's cache.")
        insertObject[B](parent.treePath, id, source, ownerID)
    }

    override def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, source: B, ownerID: String): ObjectSyncNode[B] = {
        val wrapperNode = findNode[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        genSynchronizedObject[B](wrapperNode, id, source)(ownerID)
    }

    private def findNode0[B <: AnyRef](path: Array[Int]): Option[ObjectSyncNodeImpl[B]] = {
        if (!path.headOption.contains(root.id))
            return None
        var ch: ObjectSyncNodeImpl[_ <: AnyRef] = root
        for (childID <- path.drop(1)) {
            val opt = ch.getChild(childID)
            if (opt.isEmpty)
                return None
            ch = opt.get
        }
        Option(ch) match {
            case None        => None
            case Some(value) => value match {
                case node: ObjectSyncNodeImpl[B] => Some(node)
                case _                           => None
            }
        }
    }

    private def genSynchronizedObject[B <: AnyRef](parent: MutableSyncNode[_], id: Int, source: B)(ownerID: String): ObjectSyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")

        if (source.isInstanceOf[SynchronizedObject[_]])
            throw new CanNotSynchronizeException("This object is already wrapped.")

        forest.findMatchingNode(source) match {
            case Some(value: ObjectSyncNode[B]) =>
                value
            case None                           =>
                val syncObject = instantiator.newSynchronizedInstance[B](new ContentSwitcher[B](source))
                val node       = initSynchronizedObject[B](parent, id, syncObject, source, ownerID)
                node
        }
    }

    private def initSynchronizedObject[B <: AnyRef](parent: MutableSyncNode[_], id: Int,
                                                    syncObject: B with SynchronizedObject[B], origin: AnyRef,
                                                    ownerID: String): ObjectSyncNodeImpl[B] = {
        if (syncObject.isInitialized)
            throw new IllegalSyncObjectRegistration(s"Could not register syncObject '${syncObject.getClass.getName}' : Object already initialized.")

        val data = dataFactory.newObjectData(parent.asInstanceOf[MutableSyncNode[AnyRef]], id, syncObject, Some(origin), ownerID)
        val node = new ObjectSyncNodeImpl[B](parent, data)
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

            override def findSynchronizedVersion(origin: Any): Option[SynchronizedObject[AnyRef]] = {
                cast(findMatchingSyncNode(cast(origin)).map(_.synchronizedObject))
            }

            override def initSyncObject(sync: SynchronizedObject[AnyRef]): Unit = {
                val id = sync.reference.nodePath.last
                initSynchronizedObject(node, id, sync, sync, ownerID)
            }

            override def syncObject(obj: AnyRef): SynchronizedObject[AnyRef] = {
                val id = ThreadLocalRandom.current().nextInt()
                genSynchronizedObject(node, id, obj)(ownerID).synchronizedObject
            }
        }
        node.contract.applyFieldsContracts(syncObject, manipulation)
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]

    private def createUnknownObjectNode[B <: AnyRef](path: Array[Int]): MutableSyncNode[B] = {
        val parent = getParent(path.dropRight(1))
        val data   = dataFactory.newUnknownObjectData[AnyRef](parent, path)
        val node   = new UnknownObjectSyncNode(data)
        parent.addChild(node)
        node.asInstanceOf[MutableSyncNode[B]]
    }

    private def getParent[B <: AnyRef](parentPath: Array[Int]): MutableSyncNode[B] = {
        findNode[B](parentPath).getOrElse {
            createUnknownObjectNode(parentPath)
        }
    }

    private[tree] def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int,
                                                              syncObject: B with SynchronizedObject[B], ownerID: String,
                                                              origin: Option[AnyRef]): ObjectSyncNode[B] = {
        val wrapperNode = getParent[B](parentPath)
        initSynchronizedObject[B](wrapperNode, id, syncObject, origin, ownerID)
    }

}