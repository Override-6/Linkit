package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.NoSuchCacheException
import fr.linkit.api.connection.cache.obj.description.PuppeteerInfo
import fr.linkit.api.connection.cache.obj.tree.SyncNode
import fr.linkit.api.connection.cache.obj.{EngineObjectCenter, PuppetWrapper}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.BroadcastPacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.cache.obj.NoSuchPuppetNodeException
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
        val center        = cache.nodeCenter
        val path          = puppeteerInfo.treeViewPath
        val node          = center.findNode(path).get
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
            val pos = in.position()
            val detached = node.deserialize(in)
            initialiseWrapper(detached.asInstanceOf[DetachedWrapper])
        })
    }

    protected def initialiseWrapper(detachedWrapper: DetachedWrapper): PuppetWrapper[_] = {
        val wrapped = detachedWrapper.detached
        val info    = detachedWrapper.puppeteerInfo

        val center = findCacheOfPuppeteer(info)
                .getOrElse {
                    throwNoSuchCacheException(info, if (wrapped == null) None else Some(wrapped.getClass))
                }
        if (wrapped == null) {
            val node: SyncNode[Any] = center.nodeCenter
                    .findNode[Any](info.treeViewPath)
                    .getOrElse {
                        //Replace this by a request to the sender in order to get the wrapped value.
                        throw new NoSuchPuppetNodeException(s"No puppet node found at ${info.treeViewPath.mkString("$", " -> ", "")}")
                    }
            node.puppeteer.getPuppetWrapper
        } else {
            center.injectInTreeView[Any](wrapped, info)
        }
    }

    def throwNoSuchCacheException(info: PuppeteerInfo, wrappedClass: Option[Class[_]]): Nothing = {
        throw new NoSuchCacheException(s"Could not find object center cache of id ${info.cacheID} in cache manager ${info.cacheFamily} " +
                s"in order to synchronize Wrapper object \"${wrappedClass.map(_.getName).getOrElse("Unknown Wrapped class")}\".")
    }

    private def findCacheOfPuppeteer(info: PuppeteerInfo): Option[EngineObjectCenter[Any]] = {
        val family = info.cacheFamily
        network.findCacheManager(family)
                .map(_.getCache[EngineObjectCenter[Any]](info.cacheID))
    }

}

object PuppetWrapperPersistor {

    final case class DetachedWrapper(detached: Any, puppeteerInfo: PuppeteerInfo)

}
