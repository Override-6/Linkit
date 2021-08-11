package fr.linkit.engine.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.behavior.ObjectTreeBehavior
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.tree.{SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{IllegalObjectWrapperException, SynchronizedObject, SynchronizedObjectCenter}
import fr.linkit.engine.connection.cache.obj.generation.SyncObjectInstantiationHelper
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.invokation.remote.InstancePuppeteer
import fr.linkit.engine.connection.cache.obj.tree.node.{IllegalWrapperNodeException, RootWrapperNode, WrapperNode}
import fr.linkit.engine.local.utils.ScalaUtils

import java.util.concurrent.ThreadLocalRandom

final class DefaultSynchronizedObjectTree[A <: AnyRef] private(platformIdentifier: String,
                                                               val instantiator: ObjectWrapperInstantiator,
                                                               override val id: Int,
                                                               override val center: SynchronizedObjectCenter[A],
                                                               override val behaviorTree: ObjectTreeBehavior) extends SynchronizedObjectTree[A] {

    def this(platformIdentifier: String, id: Int,
             instantiator: ObjectWrapperInstantiator, center: SynchronizedObjectCenter[A], behaviorTree: ObjectTreeBehavior)(rootSupplier: DefaultSynchronizedObjectTree[A] => RootWrapperNode[A]) = {
        this(platformIdentifier, instantiator, id, center, behaviorTree)
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
            throw new IllegalArgumentException("Parent node's is not owner by this tree's center.")
        insertObject[B](parent.treePath, id, obj, ownerID)
    }

    override def insertObject[B <: AnyRef](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        genSynchronizedObject[B](wrapperNode, id, obj, ownerID)
    }

    def registerSynchronizedObject[B <: AnyRef](parent: SyncNode[AnyRef], id: Int, wrapper: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        registerSynchronizedObject(parent.treePath, id, wrapper, ownerID)
    }

    def registerSynchronizedObject[B <: AnyRef](parentPath: Array[Int], id: Int, wrapper: B with SynchronizedObject[B], ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("/")}) (tree id == ${this.id}).")
        }
        registerSynchronizedObject[B](wrapperNode, id, wrapper, ownerID)
    }

    private def registerSynchronizedObject[B <: AnyRef](parent: WrapperNode[_], id: Int, wrapper: B with SynchronizedObject[B], ownerID: String): WrapperNode[B] = {
        val path = parent.treePath :+ id
        if (!(wrapper.getNodeInfo.nodePath sameElements path))
            throw new IllegalWrapperRegistration(s"Could not register wrapper '${wrapper.getClass.getName}' : Wrapper node's information path mismatches from given one: ${path.mkString("/")}")

        val behavior = behaviorTree.getFromClass[B](wrapper.getWrappedClass)
        if (!wrapper.isInitialized) {
            instantiator.initializeWrapper(wrapper, WrapperNodeInfo(center.family, center.cacheID, ownerID, path), behavior)
        }

        val chip                 = ObjectChip[B](behavior, wrapper)
        val puppeteer            = wrapper.getPuppeteer
        val node: WrapperNode[B] = new WrapperNode[B](puppeteer, chip, this, platformIdentifier, id, parent)
        parent.addChild(node)
        node
    }

    private def genSynchronizedObject[B <: AnyRef](parent: WrapperNode[_], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not present in this tree.")

        if (obj.isInstanceOf[SynchronizedObject[_]])
            throw new IllegalObjectWrapperException("This object is already wrapped.")

        val parentPath = parent.treePath

        val wrapperBehavior = behaviorTree.getFromClass[B](obj.getClass.asInstanceOf[Class[B]])
        val puppeteerInfo   = WrapperNodeInfo(center.family, center.cacheID, ownerID, parentPath :+ id)
        val (wrapper, _)    = instantiator.newWrapper[B](obj, behaviorTree, puppeteerInfo, Map())
        val node            = registerSynchronizedObject[B](parent, id, wrapper, ownerID)

        for (bhv <- wrapperBehavior.listField() if bhv.isSynchronized) {
            val id         = ThreadLocalRandom.current().nextInt()
            val field      = bhv.desc.javaField
            val fieldValue = field.get(obj)
            val syncValue  = genSynchronizedObject[AnyRef](node, id, fieldValue, ownerID).synchronizedObject
            ScalaUtils.setValue(wrapper, field, syncValue)
        }
        node
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