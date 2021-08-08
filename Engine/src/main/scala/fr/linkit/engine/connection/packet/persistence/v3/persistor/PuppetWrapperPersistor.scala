package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.BroadcastPacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationInputStream, DeserializationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.UnexpectedObjectException
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.WrapperInfo
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode

class PuppetWrapperPersistor(network: Network) extends ObjectPersistor[PuppetWrapper[AnyRef]] {

    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> (true, Seq(SerialisationMethod.Serial)), classOf[WrapperInfo] -> (false, Seq(SerialisationMethod.Deserial)))

    override def getSerialNode(wrapper: PuppetWrapper[AnyRef], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val wrapperNodeInfo = wrapper.getNodeInfo
        val cache           = findCache(wrapperNodeInfo).getOrElse(throwNoSuchCacheException(wrapperNodeInfo, Option(wrapper.getWrappedClass)))
        val tree            = cache.treeCenter.findTree(wrapperNodeInfo.nodePath.head).get //TODO orElseThrow
        val path            = wrapperNodeInfo.nodePath
        val node            = tree.findNode(path).get
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

        progress.pool.addWrappedClassHeader(wrapper.getWrappedClass)
        val wrapperNode = if (useInstancePointerOnly) {
            progress.getSerializationNode(WrapperInfo(wrapperNodeInfo))
        } else {
            DefaultObjectPersistor.getSerialNode(wrapper, desc, context, progress)
        }
        SimpleObjectSerializerNode(wrapperNode)
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val node = DefaultObjectPersistor.getDeserialNode(desc, context, progress)
        //println(s"Deserialize wrapper...")
        new SimpleObjectDeserializerNode() {
            override def deserializeAction(in: DeserializationInputStream): Any = {
                in.limit(in.capacity())
                //in.position(in.position() + 1)
                //val node = progress.getNextDeserializationNode
                val wrapperOrInfo = node.deserialize(in)
                val wrapper       = wrapperOrInfo match {
                    case info: WrapperInfo         => retrieveWrapper(info.nodeInfo)
                    case wrapper: PuppetWrapper[_] => wrapper
                }
                //setReference(wrapper)
                wrapper
            }
        }
    }

    override def useSortedDeserializedObjects: Boolean = true

    override def sortedDeserializedObjects(objects: Array[Any]): Unit = objects.map {
        case wrapper: PuppetWrapper[AnyRef] => wrapper
        case null                           => throw new UnexpectedObjectException("Unexpected null PuppetWrapper.")
        case other                          => throw new UnexpectedObjectException(s"Unexpected object of type '${other.getClass}', only PuppetWrapper can be handled by this PuppetWrapperPersistor.")
    }.sortBy(_.getNodeInfo.nodePath.length)
            .foreach(registerWrapper)

    private def retrieveWrapper(info: WrapperNodeInfo): PuppetWrapper[_] = {
        val path                   = info.nodePath
        val center                 = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, None)
                }
        val treeID                 = path.head
        val tree                   = center.treeCenter.findTree(treeID).get
        val node: SyncNode[AnyRef] = tree.findNode[AnyRef](path)
                .getOrElse {
                    //Replace this by a request to the sender in order to get the wrapped value.
                    throw new NoSuchWrapperNodeException(s"No puppet node found at ${info.nodePath.mkString("/")}")
                }
        node.synchronizedObject
    }

    private def registerWrapper(wrapper: PuppetWrapper[AnyRef]): Unit = {
        val info = wrapper.getNodeInfo
        val path = info.nodePath

        if (path.length == 1)
            return //root objects are automatically registered whn creating an object tree.

        val center = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, Some(wrapper.getWrappedClass))
                }
        val tree   = center.treeCenter.findTreeInternal(path.head).getOrElse {
            throw new NoSuchWrapperNodeException(s"No Object Tree found of id ${path.head}") //TODO Replace with NoSuchObjectTreeException
        }

        val nodeOpt = tree.findNode(path)
        if (nodeOpt.isEmpty) {
            tree.registerSynchronizedObject(path.dropRight(1), path.last, wrapper, info.owner).synchronizedObject
        } else if (nodeOpt.get.synchronizedObject ne wrapper) {
            throw new UnexpectedObjectException(s"Synchronized object already exists at path ${path.mkString("/")}")
        }
    }

    private def throwNoSuchCacheException(info: WrapperNodeInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in synchronized object cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
                s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }

    private def findCache(info: WrapperNodeInfo): Option[DefaultSynchronizedObjectCenter[AnyRef]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCache[DefaultSynchronizedObjectCenter[AnyRef]](info.cacheID))
    }

}

object PuppetWrapperPersistor {

    final case class WrapperInfo(nodeInfo: WrapperNodeInfo)

}