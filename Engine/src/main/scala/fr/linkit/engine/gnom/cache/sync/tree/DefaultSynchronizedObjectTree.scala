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

import fr.linkit.api.gnom.cache.sync.contract.behavior.SynchronizedObjectContractFactory
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceInstantiator
import fr.linkit.api.gnom.cache.sync.tree.{NoSuchSyncNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.api.gnom.network.Network
import fr.linkit.engine.gnom.cache.sync.instantiation.ContentSwitcher
import fr.linkit.engine.gnom.cache.sync.tree.node.{IllegalWrapperNodeException, ObjectSyncNode, RootObjectSyncNode, SyncNodeDataFactory}
import fr.linkit.engine.internal.utils.ScalaUtils

import java.util.concurrent.ThreadLocalRandom
import scala.util.Try

final class DefaultSynchronizedObjectTree[A <: AnyRef] private(currentIdentifier: String,
                                                               network: Network,
                                                               center: DefaultSyncObjectForest[A],
                                                               val instantiator: SyncInstanceInstantiator,
                                                               val dataFactory: SyncNodeDataFactory,
                                                               override val id: Int,
                                                               override val behaviorFactory: SynchronizedObjectContractFactory) extends SynchronizedObjectTree[A] {

    private var root: RootObjectSyncNode[A] = _

    def this(currentIdentifier: String,
             network: Network,
             center: DefaultSyncObjectForest[A],
             id: Int,
             instantiator: SyncInstanceInstantiator,
             dataFactory: SyncNodeDataFactory,
             behaviorTree: SynchronizedObjectContractFactory)(rootSupplier: DefaultSynchronizedObjectTree[A] => RootObjectSyncNode[A]) = {
        this(currentIdentifier, network, center, instantiator, dataFactory, id, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalWrapperNodeException("Root node's tree != this")
        if (root.id != id)
            throw new IllegalWrapperNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
        scanSyncObjectFields(root, currentIdentifier, root.synchronizedObject)
    }

    def getRoot: RootObjectSyncNode[A] = root

    override def rootNode: SyncNode[A] = root

    override def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]] = {
        checkPath(path)
        findGrandChild[B](path)
    }

    def findMatchingSyncNode(nonSyncObject: AnyRef): Option[SyncNode[_ <: AnyRef]] = {
        nonSyncObject match {
            case sync: SynchronizedObject[_] => findNode(sync.reference.nodePath)
            case nonSync                     =>
                Option(root.getMatchingSyncNode(nonSync))
        }
    }

    private def checkPath(path: Array[Int]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }

    override def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, source: B, ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owner by this tree's cache.")
        insertObject[B](parent.treePath, id, source, ownerID)
    }

    override def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, source: B, ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        genSynchronizedObject[B](wrapperNode, id, source)(ownerID)
    }

    private def findGrandChild[B <: AnyRef](path: Array[Int]): Option[ObjectSyncNode[B]] = {
        if (!path.headOption.contains(root.id))
            return None
        var ch: ObjectSyncNode[_ <: AnyRef] = root
        for (childID <- path.drop(1)) {
            val opt = ch.getChild(childID)
            if (opt.isEmpty)
                return None
            ch = opt.get
        }
        Option(ch) match {
            case None        => None
            case Some(value) => value match {
                case node: ObjectSyncNode[B] => Some(node)
                case _                       => None
            }
        }
    }

    private def genSynchronizedObject[B <: AnyRef](parent: ObjectSyncNode[_], id: Int, source: B)(ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")

        if (source.isInstanceOf[SynchronizedObject[_]])
            throw new CanNotSynchronizeException("This object is already wrapped.")

        center.findMatchingNode(source) match {
            case Some(value: SyncNode[B]) =>
                value
            case None                     =>
                val syncObject = instantiator.newWrapper[B](new ContentSwitcher[B](source))
                val node       = initSynchronizedObject[B](parent, id, syncObject, source, ownerID)
                node
        }
    }

    private def initSynchronizedObject[B <: AnyRef](parent: ObjectSyncNode[_], id: Int,
                                                    syncObject: B with SynchronizedObject[B], origin: AnyRef,
                                                    ownerID: String): ObjectSyncNode[B] = {
        if (syncObject.isInitialized)
            throw new IllegalSyncObjectRegistration(s"Could not register syncObject '${syncObject.getClass.getName}' : Object already initialized.")

        val data = dataFactory.newData(parent, id, syncObject, Some(origin), ownerID)
        val node = new ObjectSyncNode[B](parent, data)
        center.registerReference(node.reference)
        parent.addChild(node)

        scanSyncObjectFields(node, ownerID, syncObject)
        node
    }

    @inline
    private def scanSyncObjectFields[B <: AnyRef](node: ObjectSyncNode[_], ownerID: String, syncObject: B with SynchronizedObject[B]): Unit = {
        val isCurrentOwner = ownerID == currentIdentifier
        val engine         = if (!isCurrentOwner) Try(network.findEngine(ownerID).get).getOrElse(null) else null
        val behavior       = syncObject.getContract
        for (bhv <- behavior.listField()) {
            val field      = bhv.desc.javaField
            var fieldValue = ScalaUtils.getValueAnyRef(syncObject, field)
            fieldValue = findMatchingSyncNode(fieldValue).map(_.synchronizedObject).getOrElse(fieldValue)
            val modifier = bhv.modifier
            if (modifier != null) {
                fieldValue = modifier.receivedFromRemote(fieldValue, syncObject, engine)
            }
            if (bhv.isActivated && fieldValue != null) {
                fieldValue = fieldValue match {
                    case sync: SynchronizedObject[AnyRef] =>
                        if (!sync.isInitialized) {
                            val id = sync.reference.nodePath.last
                            initSynchronizedObject(node, id, sync, sync, ownerID)
                        }
                        val modifier = sync.getContract.multiModifier
                        modifier.modifyForField(sync, cast(field.getType))(syncObject, engine)
                    case _                                =>
                        val id = ThreadLocalRandom.current().nextInt()
                        genSynchronizedObject(node, id, fieldValue)(ownerID).synchronizedObject
                }
            }
            ScalaUtils.setValue(syncObject, field, fieldValue)
        }
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]

    def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int,
                                                syncObject: B with SynchronizedObject[B], ownerID: String,
                                                origin: Option[AnyRef]): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new NoSuchSyncNodeException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        initSynchronizedObject[B](wrapperNode, id, syncObject, origin, ownerID)
    }

}