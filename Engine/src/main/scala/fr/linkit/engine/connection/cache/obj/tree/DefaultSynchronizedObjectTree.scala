package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.tree.{NoSuchSyncNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.api.connection.network.Network
import fr.linkit.engine.connection.cache.obj.instantiation.ContentSwitcher
import fr.linkit.engine.connection.cache.obj.tree.node.{IllegalWrapperNodeException, ObjectSyncNode, RootObjectSyncNode, SyncNodeDataFactory}
import fr.linkit.engine.local.utils.ScalaUtils

import scala.util.Try

final class DefaultSynchronizedObjectTree[A <: AnyRef] private(currentIdentifier: String,
                                                               network: Network,
                                                               val instantiator: ObjectWrapperInstantiator,
                                                               val dataFactory: SyncNodeDataFactory,
                                                               override val id: Int,
                                                               override val behaviorStore: ObjectBehaviorStore) extends SynchronizedObjectTree[A] {

    private var root: RootObjectSyncNode[A] = _

    def this(currentIdentifier: String, network: Network, id: Int, instantiator: ObjectWrapperInstantiator, dataFactory: SyncNodeDataFactory, behaviorTree: ObjectBehaviorStore)(rootSupplier: DefaultSynchronizedObjectTree[A] => RootObjectSyncNode[A]) = {
        this(currentIdentifier, network, instantiator, dataFactory, id, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalWrapperNodeException("Root node's tree != this")

        if (root.id != id)
            throw new IllegalWrapperNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
    }

    def getRoot: RootObjectSyncNode[A] = root

    override def rootNode: SyncNode[A] = root

    override def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]] = {
        checkPath(path)
        findGrandChild[B](path)
    }

    private def checkPath(path: Array[Int]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }

    override def insertObject[B <: AnyRef](parent: SyncNode[_], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owner by this tree's cache.")
        insertObject[B](parent.treePath, id, obj, ownerID)
    }

    override def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        genSynchronizedObject[B](wrapperNode, id, obj)(ownerID)
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

    private def genSynchronizedObject[B <: AnyRef](parent: ObjectSyncNode[_], id: Int, obj: B)(ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")

        if (obj.isInstanceOf[SynchronizedObject[_]])
            throw new CanNotSynchronizeException("This object is already wrapped.")

        val syncObject    = instantiator.newWrapper[B](new ContentSwitcher[B](obj))
        val node          = initSynchronizedObject[B](parent, id, syncObject, ownerID)
        node
    }

    private def initSynchronizedObject[B <: AnyRef](parent: ObjectSyncNode[_], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): ObjectSyncNode[B] = {
        if (!syncObject.isInitialized)
            throw new IllegalSyncObjectRegistration(s"Could not register syncObject '${syncObject.getClass.getName}' : Object already initialized.")

        val data = dataFactory.newData(parent, id, syncObject, ownerID)
        val node: ObjectSyncNode[B] = new ObjectSyncNode[B](parent, data)
        //network.rootRefStore += (nodeInfo.hashCode(), syncObject)
        parent.addChild(node)

        scanSyncObjectFields(ownerID, syncObject)

        node
    }

    @inline
    private def scanSyncObjectFields(ownerID: String, syncObject: SynchronizedObject[_]): Unit = {
        val isCurrentOwner = ownerID == currentIdentifier
        val engine         = if (!isCurrentOwner) Try(network.findEngine(ownerID).get).getOrElse(null) else null //should not be used if isCurrentOwner = false
        val behavior       = behaviorStore.getFromClass(syncObject.getSuperClass)
        for (bhv <- behavior.listField()) {
            val field      = bhv.desc.javaField
            val fieldValue = field.get(syncObject)
            val finalField = {
                if (isCurrentOwner) behaviorStore.modifyFieldForLocalComingFromRemote(syncObject, engine, fieldValue, bhv)
                else behaviorStore.modifyFieldForLocalComingFromRemote(syncObject, engine, fieldValue, bhv)
            }
            ScalaUtils.setValue(syncObject, field, finalField)
        }
    }

    def registerSynchronizedObject[B <: AnyRef](parent: SyncNode[AnyRef], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        registerSynchronizedObject(parent.treePath, id, syncObject, ownerID)
    }

    def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new NoSuchSyncNodeException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        initSynchronizedObject[B](wrapperNode, id, syncObject, ownerID)
    }

}