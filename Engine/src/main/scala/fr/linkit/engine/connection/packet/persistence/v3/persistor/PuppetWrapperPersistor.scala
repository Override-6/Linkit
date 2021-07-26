package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.api.connection.cache.repo.description.PuppeteerInfo
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.{DeserializerNode, ObjectDeserializerNode}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext, SerializableClassDescription}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.DetachedWrapper
import fr.linkit.engine.local.utils.ScalaUtils

class PuppetWrapperPersistor(network: Network) extends ObjectPersistor[PuppetWrapper[_]] {
    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> true, classOf[DetachedWrapper] -> false)

    override def getSerialNode(wrapper: PuppetWrapper[_], desc: SerializableClassDescription, context: PersistenceContext, progress: SerialisationProgression): SerializerNode = {
        val detached = DetachedWrapper(wrapper.detachedSnapshot(), wrapper.getPuppeteerInfo)
        progress.getSerializationNode(detached)
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val ref = DetachedWrapper(null, null)
        SimpleObjectDeserializerNode(ref)(in => {
            val detached = in.readObject[DetachedWrapper]()
            initialiseWrapper(detached)
            ScalaUtils.pasteAllFields(ref, detached)
        })
    }

    protected def initialiseWrapper(detachedWrapper: DetachedWrapper): PuppetWrapper[_] = {
        val wrapped = detachedWrapper.detached
        val info    = detachedWrapper.puppeteerInfo
        val family  = info.cacheFamily
        val opt     = network.getCacheManager(family)
        if (opt.isEmpty) {
            AppLogger.error(s"Could not synchronize Wrapper object ${wrapped.getClass} because no cache of family $family. The object is returned as null.")
            return null
        }
        opt.get
            .getCacheAsync(info.cacheID, DefaultEngineObjectCenter[Any]())
            .initAsWrapper(wrapped, info)
    }

}

object PuppetWrapperPersistor {

    final case class DetachedWrapper(detached: Any, puppeteerInfo: PuppeteerInfo)

}
