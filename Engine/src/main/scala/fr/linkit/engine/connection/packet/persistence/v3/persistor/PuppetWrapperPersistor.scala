package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.BroadcastPacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter
import fr.linkit.engine.connection.cache.obj.generation.CloneHelper
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.UnexpectedObjectException
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.DetachedWrapper
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode

import scala.collection.mutable

class PuppetWrapperPersistor(network: Network) extends ObjectPersistor[PuppetWrapper[_]] {

    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> (true, Seq(SerialisationMethod.Serial)), classOf[DetachedWrapper] -> (false, Seq(SerialisationMethod.Deserial)))

    private val awfulMap = mutable.HashMap.empty[Array[Int], Any]

    override def getSerialNode(wrapper: PuppetWrapper[_], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val puppeteerInfo = wrapper.getWrapperNodeInfo
        val cache         = findCache(puppeteerInfo).getOrElse(throwNoSuchCacheException(puppeteerInfo, Option(wrapper.getWrappedClass)))
        val tree          = cache.treeCenter.findTree(puppeteerInfo.nodePath.head).get //TODO orElseThrow
        val path          = puppeteerInfo.nodePath
        val node          = tree.findNode(path).get
        //root objects are present on all clients.

        val check: String => Boolean = { engineID => {
            val isPresent = node.isPresentOnEngine(engineID)
            //As this wrapper is not present on the targeted engine, the object value will be send.
            if (!isPresent)
                node.putPresence(engineID)
            isPresent
        }
        }
        val useInstancePointerOnly   = progress.coordinates match {
            case BroadcastPacketCoordinates(_, _, discardTargets, targetIDs) if discardTargets =>
                network.listEngines
                        .map(_.identifier)
                        .filterNot(targetIDs.contains)
                        .forall(check)

            case other => other.forallConcernedTargets(check)
        }

        val detachedWrapper = DetachedWrapper(if (useInstancePointerOnly) null else CloneHelper.detachedWrapperClone(wrapper, false), puppeteerInfo)
        SimpleObjectSerializerNode(progress.getSerializationNode(detachedWrapper))
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val node = DefaultObjectPersistor.getDeserialNode(desc, context, progress)
        //println(s"Deserialize wrapper...")
        new SimpleObjectDeserializerNode(in => {
            val detached = node.deserialize(in)
            extractWrapper(detached.asInstanceOf[DetachedWrapper])
        })
    }

    override def useSortedDeserializedObjects: Boolean = true

    override def sortedDeserializedObjects(objects: Array[Any]): Unit = objects.foreach {
        case wrapper: PuppetWrapper[Any] => registerWrapper(wrapper)
        case null                        => throw new UnexpectedObjectException("Unexpected null PuppetWrapper.")
        case other                       => throw new UnexpectedObjectException(s"Unexpected object of type '${other.getClass}', only PuppetWrapper can be handled by this PuppetWrapperPersistor.")
    }

    private def extractWrapper(detachedWrapper: DetachedWrapper): PuppetWrapper[_] = {
        val wrapped = detachedWrapper.detached
        val info    = detachedWrapper.puppeteerInfo
        val path    = info.nodePath
        val center  = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, if (wrapped == null) None else Some(wrapped.getClass))
                }
        val treeID  = path.head
        if (wrapped == null) {
            val tree                = center.treeCenter.findTree(treeID).get
            val node: SyncNode[Any] = tree.findNode[Any](path)
                    .getOrElse {
                        //Replace this by a request to the sender in order to get the wrapped value.
                        throw new NoSuchWrapperNodeException(s"No puppet node found at ${info.nodePath.mkString("$", " -> ", "")}")
                    }
            node.synchronizedObject
        } else {
            val isNotRoot = path.length != 1
            val tree      = center.treeCenter.findTreeInternal(treeID).getOrElse {
                if (isNotRoot)
                    throw new NoSuchWrapperNodeException(s"Could not find Object tree '$treeID'")
                center.createNewTree(treeID, info.owner, wrapped)
            }
            if (isNotRoot)
                tree.instantiator.newWrapper(wrapped, tree.behaviorTree, info)
            else
                tree.rootNode.synchronizedObject
        }
    }

    private def registerWrapper(wrapper: PuppetWrapper[Any]): Unit = {
        val info = wrapper.getWrapperNodeInfo
        val path = info.nodePath

        if (path.length == 1)
            return //root objects are automatically registered whn creating an object tree.

        val center = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, Some(wrapper.getWrappedClass))
                }
        val tree   = center.treeCenter.findTreeInternal(path.head).get

        if (tree.findNode(path).isEmpty) {
            tree.registerSynchronizedObject(path.dropRight(1), path.last, wrapper, info.owner).synchronizedObject
        }
    }

    private def throwNoSuchCacheException(info: WrapperNodeInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
                s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }

    private def findCache(info: WrapperNodeInfo): Option[DefaultSynchronizedObjectCenter[Any]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCache[DefaultSynchronizedObjectCenter[Any]](info.cacheID))
    }

}

object PuppetWrapperPersistor {

    final case class DetachedWrapper(detached: Any, puppeteerInfo: WrapperNodeInfo)

}
