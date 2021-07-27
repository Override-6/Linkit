package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.cache.repo.description.PuppeteerInfo
import fr.linkit.api.connection.cache.repo.{EngineObjectCenter, PuppetWrapper}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.PacketSerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor.DetachedWrapper
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode

class PuppetWrapperPersistor(network: Network) extends ObjectPersistor[PuppetWrapper[_]] {

    override val handledClasses: Seq[HandledClass] = Seq(classOf[PuppetWrapper[_]] -> (true, Seq(SerialisationMethod.Serial)), classOf[DetachedWrapper] -> (false, Seq(SerialisationMethod.Deserial)))

    override def getSerialNode(wrapper: PuppetWrapper[_], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: PacketSerialisationProgression): ObjectSerializerNode = {
        val puppeteerInfo = wrapper.getPuppeteerInfo
        val cache = getCacheOfPuppeteer(puppeteerInfo, wrapper.getWrappedClass)
        val center = cache.center
        val node = center.findNode(puppeteerInfo.treeViewPath).get
        //val detached = if (node.isPresentOnEngine(pro))

        val detachedWrapper = DetachedWrapper(wrapper.detachedSnapshot(), puppeteerInfo)
        SimpleObjectSerializerNode(progress.getSerializationNode(detachedWrapper))
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val node = DefaultObjectPersistor.getDeserialNode(desc, context, progress)
        //println(s"Deserialize wrapper...")
        new SimpleObjectDeserializerNode(in => {
            val detached = node.deserialize(in)
            initialiseWrapper(detached.asInstanceOf[DetachedWrapper])
        })
    }

    protected def initialiseWrapper(detachedWrapper: DetachedWrapper): PuppetWrapper[_] = {
        val wrapped = detachedWrapper.detached
        //println(s"initializing wrapped ${wrapped}")
        val info    = detachedWrapper.puppeteerInfo
        getCacheOfPuppeteer(info, wrapped.getClass)
                .initAsWrapper[Any](wrapped, info)
    }

    private def getCacheOfPuppeteer(info: PuppeteerInfo, wrapperClass: Class[_]): EngineObjectCenter[Any] = {
        val family = info.cacheFamily
        val opt = network.getCacheManager(family)
        if (opt.isEmpty) {
            AppLogger.error(s"Could not synchronize Wrapper object ${wrapperClass.getName} because no cache of family $family. The object is returned as null.")
            return null
        }
        opt.get
                .getCache(info.cacheID, DefaultEngineObjectCenter[Any]())
    }

}

object PuppetWrapperPersistor {

    final case class DetachedWrapper(detached: Any, puppeteerInfo: PuppeteerInfo)

}
