package fr.linkit.engine.connection.cache.obj.tree

import java.util.concurrent.ThreadLocalRandom

import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, PuppeteerInfo}
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.api.connection.cache.obj.tree.{SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{IllegalObjectWrapperException, PuppetWrapper, SynchronizedObjectCenter}
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip
import fr.linkit.engine.connection.cache.obj.tree.DefaultSynchronizedObjectTree.WaitingNode
import fr.linkit.engine.local.utils.ScalaUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultSynchronizedObjectTree[A] private(platformIdentifier: String,
                                               instantiator: ObjectWrapperInstantiator,
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

    /**
     * This map stores every children that attempted to attach to a parent that
     * does not yet exists. <br>
     * When children nodes are in this map, they they have no effect on the outside world (no remote method handling etc...).
     * This map is used during packet deserialisation in case of nested synchronised objects
     * The Array[Int] key is the parent's path, and the value is the children's supplier (see [[findNode()]])
     *
     * @see findNode()
     */
    private val waitingChildren = mutable.HashMap.empty[Array[Int], ListBuffer[WaitingNode]]

    override def findNode[B](path: Array[Int]): Option[SyncNode[B]] = {
        checkPath(path)
        findGrandChild[B](path)
    }

    override def insertObject[B](parent: SyncNode[_], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owner by this tree's center.")
        insertObject[B](parent.treePath, id, obj, ownerID)
    }

    override def insertObject[B](parentPath: Array[Int], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        val wrapperNode = findGrandChild[B](parentPath).getOrElse {
            throw new IllegalArgumentException(s"Could not find parent path in this object tree (${parentPath.mkString("$", " -> ", "")}) (tree id == ${this.id}).")
        }
        genSynchronizedObject[B](wrapperNode, id, obj, ownerID)
    }

    private def genSynchronizedObject[B](parent: WrapperNode[_], id: Int, obj: B, ownerID: String): SyncNode[B] = {
        if (parent.tree ne this)
            throw new IllegalArgumentException("Parent node's is not owned by this tree's center.")

        if (obj.isInstanceOf[PuppetWrapper[_]])
            throw new IllegalObjectWrapperException("This object is already wrapped.")

        val parentPath = parent.treePath

        def registerObject(wrapper: PuppetWrapper[B]): WrapperNode[B] = {
            val behavior = behaviorTree.getFromClass[B](wrapper.getWrappedClass)

            val chip                 = ObjectChip[B](ownerID, behavior, wrapper)
            val puppeteer            = wrapper.getPuppeteer
            val node: WrapperNode[B] = new WrapperNode[B](puppeteer, chip, this, platformIdentifier, id, parent)
            parent.addChild(node)
            node
        }

        val wrapperBehavior = behaviorTree.getFromClass[B](obj.getClass.asInstanceOf[Class[B]])
        val puppeteerInfo   = PuppeteerInfo(center.family, center.cacheID, ownerID, parentPath :+ id)
        val wrapper         = instantiator.newWrapper(obj, behaviorTree, puppeteerInfo)
        val node            = registerObject(wrapper)

        for (bhv <- wrapperBehavior.listField() if bhv.isSynchronized) {
            val id         = ThreadLocalRandom.current().nextInt()
            val field      = bhv.desc.javaField
            val fieldValue = field.get(obj)
            val syncValue  = genSynchronizedObject[Any](node, id, fieldValue, ownerID).synchronizedObject
            ScalaUtils.setValue(obj, field, syncValue)
        }
        node
    }

    private def findGrandChild[B](relativePath: Array[Int]): Option[WrapperNode[B]] = {
        if (relativePath.headOption.exists(root.id != _))
            return None
        var ch: WrapperNode[_] = root
        for (childID <- relativePath.dropRight(1)) {
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

object DefaultSynchronizedObjectTree {

    private case class WaitingNode(id: Int, supplier: SyncNode[_] => WrapperNode[_]) {
        def awake(parent: SyncNode[_]): WrapperNode[_] = supplier(parent)
    }

}
