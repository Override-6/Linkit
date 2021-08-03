package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperClassCenter
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.BroadcastPacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationInputStream, DeserializationProgression}
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.cache.obj.DefaultSynchronizedObjectCenter
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.UnexpectedObjectException
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.{PuppetWrapperDeserializerNode, SimpleObjectDeserializerNode}
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.WrapperPointer
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.{NullInstanceNode, SimpleObjectSerializerNode}
import fr.linkit.engine.local.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

class PuppetWrapperPersistor(network: Network, center: ObjectWrapperClassCenter) extends ObjectPersistor[PuppetWrapper[AnyRef]] {

    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> (true, Seq(SerialisationMethod.Serial)), classOf[WrapperPointer] -> (false, Seq(SerialisationMethod.Deserial)))

    override def getSerialNode(wrapper: PuppetWrapper[AnyRef], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val puppeteerInfo = wrapper.getNodeInfo
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

        val detachedWrapper = WrapperPointer(wrapper.getWrappedClass, puppeteerInfo)
        val pointerNode     = progress.getSerializationNode(detachedWrapper)
        val wrapperNode     = if (useInstancePointerOnly) new NullInstanceNode(false) else {
            progress.getSerializationNode(wrapper)
        }
        SimpleObjectSerializerNode(out => {
            pointerNode.writeBytes(out)
            wrapperNode.writeBytes(out)
            ()
        })
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {

        //println(s"Deserialize wrapper...")
        new PuppetWrapperDeserializerNode() {
            override def deserializeWrapper(in: DeserializationInputStream): PuppetWrapper[_] = {
                DefaultObjectPersistor.getCustomDeserialNode(ref).deserialize(in).asInstanceOf[PuppetWrapper[_]]
            }

            override def retrieveAndSetWrapperRef(in: DeserializationInputStream): Unit = {
                val buff = in.buff
                buff.limit(buff.capacity())
                //buff.position(buff.position() - 1)
                val pointer = DefaultObjectPersistor.getDeserialNode(desc, context, progress).deserialize(in).asInstanceOf[WrapperPointer]
                val clazz   = center.getWrapperClass(pointer.detachedClass) //Just load the class.
                buff.limit(buff.capacity())
                val ref = ScalaUtils.allocate[AnyRef](clazz)
                //setReference(ref)
                val wrapperNode = progress.getNextDeserializationNode
                val wrapper = wrapperNode match {
                    case wrapperNode: PuppetWrapperDeserializerNode => wrapperNode.deserializeWrapper(ref, in)
                    case objectNode: SimpleObjectDeserializerNode => objectNode.deserialize(in)
                }
                wrapper
            }
        }
    }

    override def useSortedDeserializedObjects: Boolean = true

    override def sortedDeserializedObjects(objects: Array[Any]): Unit = objects.foreach {
        case wrapper: PuppetWrapper[AnyRef] => registerWrapper(wrapper)
        case null                           => throw new UnexpectedObjectException("Unexpected null PuppetWrapper.")
        case other                          => throw new UnexpectedObjectException(s"Unexpected object of type '${other.getClass}', only PuppetWrapper can be handled by this PuppetWrapperPersistor.")
    }

    private def extractWrapper(pointer: WrapperPointer, @Nullable wrapper: PuppetWrapper[AnyRef]): PuppetWrapper[_] = {
        val info   = pointer.puppeteerInfo
        val path   = info.nodePath
        val center = findCache(info)
                .getOrElse {
                    throwNoSuchCacheException(info, if (wrapper == null) None else Some(wrapper.getWrappedClass))
                }
        val treeID = path.head
        if (wrapper == null) {
            val tree                   = center.treeCenter.findTree(treeID).get
            val node: SyncNode[AnyRef] = tree.findNode[AnyRef](path)
                    .getOrElse {
                        //Replace this by a request to the sender in order to get the wrapped value.
                        throw new NoSuchWrapperNodeException(s"No puppet node found at ${info.nodePath.mkString("/")}")
                    }
            node.synchronizedObject
        } else {
            val tree = center.treeCenter.findTreeInternal(treeID).getOrElse {
                throw new NoSuchWrapperNodeException(s"Could not find Object tree '$treeID'")
            }
            val node = tree.registerSynchronizedObject(path.dropRight(1), path.last, wrapper, info.owner)
            node.synchronizedObject
        }
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
        val tree   = center.treeCenter.findTreeInternal(path.head).get

        if (tree.findNode(path).isEmpty) {
            tree.registerSynchronizedObject(path.dropRight(1), path.last, wrapper, info.owner).synchronizedObject
        }
    }

    private def throwNoSuchCacheException(info: WrapperNodeInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
                s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }

    private def findCache(info: WrapperNodeInfo): Option[DefaultSynchronizedObjectCenter[AnyRef]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCache[DefaultSynchronizedObjectCenter[AnyRef]](info.cacheID))
    }

}

object PuppetWrapperPersistor {

    final case class WrapperPointer(detachedClass: Class[_], puppeteerInfo: WrapperNodeInfo)

}
