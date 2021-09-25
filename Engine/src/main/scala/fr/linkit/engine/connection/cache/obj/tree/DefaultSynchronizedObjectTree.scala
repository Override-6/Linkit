package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.behavior.ObjectBehaviorStore
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.tree.{NoSuchSyncNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{CanNotSynchronizeException, SynchronizedObject, SynchronizedObjectCache}
import fr.linkit.engine.connection.cache.obj.instantiation.ContentSwitcher
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.tree.node.{IllegalWrapperNodeException, RootWrapperNode, WrapperNode}
import fr.linkit.engine.local.utils.ScalaUtils

import scala.util.Try

final class DefaultSynchronizedObjectTree[A <: AnyRef] private(currentIdentifier: String,
                                                               val instantiator: ObjectWrapperInstantiator,
                                                               override val id: Int,
                                                               override val cache: SynchronizedObjectCache[A],
                                                               override val behaviorStore: ObjectBehaviorStore) extends SynchronizedObjectTree[A] {

    private val network = cache.network

    def this(platformIdentifier: String, id: Int,
             instantiator: ObjectWrapperInstantiator, cache: SynchronizedObjectCache[A], behaviorTree: ObjectBehaviorStore)(rootSupplier: DefaultSynchronizedObjectTree[A] => RootWrapperNode[A]) = {
        this(platformIdentifier, instantiator, id, cache, behaviorTree)
        val root = rootSupplier(this)
        if (root.tree ne this)
            throw new IllegalWrapperNodeException("Root node's tree != this")

        if (root.id != id)
            throw new IllegalWrapperNodeException("Root's identifier is not equals to this tree's identifier.")
        this.root = root
    }

    private var root: RootWrapperNode[A] = _

    def getRoot: RootWrapperNode[A] = root

    override def rootNode: SyncNode[A] = root

    override def findNode[B <: AnyRef](path: Array[Int]): Option[SyncNode[B]] = {
        checkPath(path)
        findGrandChild[B](path)
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

    def registerSynchronizedObject[B <: AnyRef](parent: SyncNode[AnyRef], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        registerSynchronizedObject(parent.treePath, id, syncObject, ownerID)
    }

    def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new NoSuchSyncNodeException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        registerSynchronizedObject[B](wrapperNode, id, syncObject, ownerID)
    }

    private def registerSynchronizedObject[B <: AnyRef](parent: WrapperNode[_], id: Int, syncObject: B with SynchronizedObject[B], ownerID: String): WrapperNode[B] = {
        val path = parent.treePath :+ id
        if (!(syncObject.getNodeInfo.nodePath sameElements path))
            throw new IllegalWrapperRegistration(s"Could not register syncObject '${syncObject.getClass.getName}' : Wrapper node's information path mismatches from given one: ${path.mkString("/")}")

        val behavior = behaviorStore.getFromClass[B](syncObject.getSuperClass)
        if (!syncObject.isInitialized) {
            instantiator.initializeSyncObject(syncObject, SyncNodeInfo(cache.family, cache.cacheID, ownerID, path), behaviorStore)
        }

        scanSyncObjectFields(ownerID, syncObject)

        val chip                 = ObjectChip[B](behavior, cache.network, syncObject)
        val puppeteer            = syncObject.getPuppeteer
        val node: WrapperNode[B] = new WrapperNode[B](puppeteer, chip, this, currentIdentifier, id, parent)
        val nodeInfo = puppeteer.nodeInfo
        network.rootRefStore += (nodeInfo.hashCode(), syncObject)
        parent.addChild(node)
        node
    }

    private def genSynchronizedObject[B <: AnyRef](parent: WrapperNode[_], id: Int, obj: B)(ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")

        if (obj.isInstanceOf[SynchronizedObject[_]])
            throw new CanNotSynchronizeException("This object is already wrapped.")

        val parentPath    = parent.treePath
        val puppeteerInfo = SyncNodeInfo(cache.family, cache.cacheID, ownerID, parentPath :+ id)
        val syncObject    = instantiator.newWrapper[B](new ContentSwitcher[B](obj), behaviorStore, puppeteerInfo)
        val node          = registerSynchronizedObject[B](parent, id, syncObject, ownerID)
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

    private def findGrandChild[B <: AnyRef](path: Array[Int]): Option[WrapperNode[B]] = {
        if (!path.headOption.contains(root.id))
            return None
        var ch: WrapperNode[_ <: AnyRef] = root
        for (childID <- path.drop(1)) {
            val opt = ch.getChild(childID)
            if (opt.isEmpty)
                return None
            ch = opt.get
        }
        Option(ch) match {
            case None        => None
            case Some(value) => value match {
                case node: WrapperNode[B] => Some(node)
                case _                    => None
            }
        }
    }

    private def checkPath(path: Array[Int]): Unit = {
        if (path.isEmpty)
            throw new InvalidNodePathException("Path is empty")
    }

}