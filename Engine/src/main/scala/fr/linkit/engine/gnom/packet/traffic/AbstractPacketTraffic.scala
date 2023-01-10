/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.network.tag.{EngineSelector, Everyone}
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.{TrafficObjectReference, TrafficReference}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.Reason
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.SimplePacketBundle
import fr.linkit.engine.gnom.packet.traffic.channel.SystemObjectManagementChannel
import fr.linkit.engine.gnom.packet.traffic.unit.SequentialInjectionProcessorUnit
import fr.linkit.engine.gnom.persistence.config.{PersistenceConfigBuilder, SimplePersistenceConfig}
import fr.linkit.engine.gnom.referencing.linker.OMCContextObjectLinker
import fr.linkit.engine.gnom.referencing.presence.SystemNetworkObjectPresence
import fr.linkit.engine.internal.debug.cli.SectionedPrinter
import fr.linkit.engine.internal.debug.{Debugger, PacketInjectionStep}
import fr.linkit.engine.internal.util.ClassMap

import java.net.URL
import scala.reflect.ClassTag

abstract class AbstractPacketTraffic(defaultPersistenceConfigUrl: Option[URL],
                                     selector: EngineSelector) extends PacketTraffic {

    private  val minimalConfigBuilder                        = PersistenceConfigBuilder.fromScript(getClass.getResource("/default_scripts/persistence_minimal.sc"), connection)
    private  val omcNode                                     = createObjectManagementChannel()
    private  val objectChannel                               = omcNode.injectable
    override  val defaultPersistenceConfig: PersistenceConfig = {
        defaultPersistenceConfigUrl
                .fold(new PersistenceConfigBuilder())(PersistenceConfigBuilder.fromScript(_, connection))
                .transfer(minimalConfigBuilder)
                .build(null, objectChannel, selector)
    }

    override  val reference  : TrafficReference      = TrafficReference
    override  val presence   : NetworkObjectPresence = SystemNetworkObjectPresence
    override  val trafficPath: Array[Int]            = Array.empty
    private   val linker                             = new TrafficNetworkObjectLinker(objectChannel, this, selector)
    protected val rootStore                          = new SimplePacketInjectableStore(this, linker, defaultPersistenceConfig, Array.empty)
    @volatile private var closed                     = false

    override def getTrafficObjectLinker: TrafficNetworkObjectLinker = linker

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): InjectableTrafficNode[C] = {
        rootStore.getInjectable[C](injectableID, config, factory, scopeFactory)
    }

    override def getPersistenceConfig(path: Array[Int]): PersistenceConfig = {
        if (path.isEmpty)
            omcNode.persistenceConfig
        else
            rootStore.getPersistenceConfig(path)
    }

    override def close(reason: Reason): Unit = {
        rootStore.close()
        closed = true
    }

    override def isClosed: Boolean = closed

    @inline
    override def processInjection(packet: Packet, attr: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit = {
        processInjection(SimplePacketBundle(packet, attr, coordinates, None))
    }

    override def processInjection(bundle: PacketBundle): Unit = {
        val path             = bundle.coords.path
        val node             = findNode(path).get
        val channelReference = node.reference
        AppLoggers.GNOM.trace(s"Injecting packet bundle ($bundle) into channel $channelReference")
        Debugger.push(PacketInjectionStep(bundle.packetID, channelReference))
        node.injectable.inject(bundle)
        Debugger.pop()
    }

    override def processInjection(result: PacketDownload): Unit = {
        if (result.isDeserialized) {
            val bundle = SimplePacketBundle(result)
            result.informInjected
            processInjection(bundle)
            return
        }
        //if not deserialized place the downloaded in corresponding processor unit to deserialize it later.
        val path = result.coords.path
        val node = findNode(path).getOrElse {
            throw new NoSuchTrafficPresenceException(s"Could not process injection: Could not find packet injectable located at ${path.mkString("/")}")
        } match {
            case node: InjectableTrafficNode[_] => node
            case _                              =>
                val reference = TrafficReference / path
                throw new ConflictException(s"Could not inject packet: Attempted to inject packet into a traffic object located at $reference, but the object's node is not an InjectableTrafficNode.")
        }
        node.unit().post(result)
    }

    override def newWriter(path: Array[Int]): PacketWriter = {
        newWriter(path, defaultPersistenceConfig)
    }

    override def findNode(path: Array[Int]): Option[TrafficNode[PacketInjectable]] = {
        if (path.isEmpty) return Some(omcNode)
        rootStore.findNode(path)
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = rootStore.findStore(id)

    override def findInjectable[C <: PacketInjectable : ClassTag](id: Int): Option[C] = rootStore.findInjectable(id)

    override def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore = rootStore.createStore(id, persistenceConfig)

    def getObjectManagementChannel: ObjectManagementChannel = objectChannel

    def findTrafficObject(reference: TrafficObjectReference): Option[TrafficObject[TrafficReference]] = {
        rootStore.findNode(reference.trafficPath).map(_.injectable)
    }

    private def createObjectManagementChannel(): InjectableTrafficNode[ObjectManagementChannel] = {
        val objectChannelConfig = {
            val linker = new OMCContextObjectLinker(minimalConfigBuilder)
            new SimplePersistenceConfig(new ClassMap(), linker)
        }
        val objectChannel       = {
            val scope = ChannelScopes(Everyone)(newWriter(Array.emptyIntArray, objectChannelConfig))
            new SystemObjectManagementChannel(scope)
        }
        new InjectableTrafficNode[ObjectManagementChannel] {
            override val injectable       : ObjectManagementChannel = objectChannel
            override val persistenceConfig: PersistenceConfig       = objectChannelConfig
            override val unit             : InjectionProcessorUnit  = new SequentialInjectionProcessorUnit(objectChannel)

            override def setPerformantInjection(): this.type = throw new UnsupportedOperationException

            override def setSequentialInjection(): this.type = throw new UnsupportedOperationException

            override def preferPerformances(): Boolean = false

            override def chainTo(path: Array[Int]): this.type = throw new UnsupportedOperationException
        }
    }

    /**
     * dumps this traffic.
     * Do not use this method directly, use [[Debugger.dumpConnectionTraffic()]] instead.
     * */
    private[linkit] def dump(printer: SectionedPrinter): Unit = {
        import printer._
        val section = printer.newSection()
        section.append("/ (ObjectManagementChannel):")
        omcNode.unit().asInstanceOf[SequentialInjectionProcessorUnit] //OMC's unit IS a SIPU
                .appendDump(printer)(section, 6)
        rootStore.appendDump(printer)(section)
        section.flush()
    }

}