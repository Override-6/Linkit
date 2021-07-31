package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.{PuppetWrapper, SynchronizedObjectCenter}
import fr.linkit.api.connection.cache.obj.description.PuppeteerInfo
import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.BroadcastPacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.cache.obj.SynchronizedObjectCenterInternal
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.DetachedWrapper
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode

import scala.collection.mutable

class PuppetWrapperPersistor(network: Network) extends ObjectPersistor[PuppetWrapper[_]] {

    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> (true, Seq(SerialisationMethod.Serial)), classOf[DetachedWrapper] -> (false, Seq(SerialisationMethod.Deserial)))

    private val awfulMap = mutable.HashMap.empty[Array[Int], Any]

    override def getSerialNode(wrapper: PuppetWrapper[_], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val puppeteerInfo = wrapper.getPuppeteerInfo
        val cache         = findCacheOfPuppeteer(puppeteerInfo).getOrElse(throwNoSuchCacheException(puppeteerInfo, Option(wrapper.getWrappedClass)))
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

        val detachedWrapper = DetachedWrapper(if (useInstancePointerOnly) null else awfulMap.getOrElseUpdate(path, wrapper.detachedSnapshot()), puppeteerInfo)
        SimpleObjectSerializerNode(progress.getSerializationNode(detachedWrapper))
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val node = DefaultObjectPersistor.getDeserialNode(desc, context, progress)
        //println(s"Deserialize wrapper...")
        new SimpleObjectDeserializerNode(in => {
            val pos      = in.position()
            val detached = node.deserialize(in)
            initialiseWrapper(detached.asInstanceOf[DetachedWrapper])
        })
    }

    protected def initialiseWrapper(detachedWrapper: DetachedWrapper): PuppetWrapper[_] = {
        val wrapped = detachedWrapper.detached
        val info    = detachedWrapper.puppeteerInfo
        val path    = info.nodePath
        val center  = findCacheOfPuppeteer(info)
            .getOrElse {
                throwNoSuchCacheException(info, if (wrapped == null) None else Some(wrapped.getClass))
            }
        val tree    = center.treeCenter.findTree(path.head).get

        if (wrapped == null) {
            val node: SyncNode[Any] = tree.findNode[Any](path)
                .getOrElse {
                    //Replace this by a request to the sender in order to get the wrapped value.
                    throw new NoSuchWrapperNodeException(s"No puppet node found at ${info.nodePath.mkString("$", " -> ", "")}")
                }
            node.puppeteer.getPuppetWrapper
        } else {
            tree.insertObject(path.dropRight(1), path.last, wrapped, info.owner).synchronizedObject
        }
    }

    def throwNoSuchCacheException(info: PuppeteerInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object tree of id ${info.nodePath.head} in cache id ${info.cacheID} from cache manager ${info.cacheFamily} " +
            s": could not properly deserialize and synchronize Wrapper object of class \"${wrappedClass.map(_.getName).getOrElse("(Unknown Wrapped class)")}\".")
    }

    private def findCacheOfPuppeteer(info: PuppeteerInfo): Option[SynchronizedObjectCenter[Any]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
            .map(_.getCache[SynchronizedObjectCenter[Any]](info.cacheID))
    }

}

object PuppetWrapperPersistor {

    final case class DetachedWrapper(detached: Any, puppeteerInfo: PuppeteerInfo)

}
