/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.packet.traffic.injection.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.ObjectDeserializationResult
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.{TrafficObjectReference, TrafficReference}
import fr.linkit.api.gnom.reference.SystemNetworkObjectPresence
import fr.linkit.api.gnom.reference.linker.NetworkObjectLinker
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.gnom.reference.traffic.{ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.internal.system.{ClosedException, Reason}
import fr.linkit.engine.gnom.packet.SimplePacketBundle
import fr.linkit.engine.gnom.packet.traffic.channel.DefaultObjectManagementChannel
import fr.linkit.engine.gnom.packet.traffic.injection.SequentialInjectionProcessorUnit
import fr.linkit.engine.gnom.persistence.config.{ImmutablePersistenceContext, PersistenceConfigBuilder, SimplePersistenceConfig}
import fr.linkit.engine.gnom.reference.linker.ObjectChannelContextObjectLinker
import fr.linkit.engine.internal.utils.ClassMap

import java.net.URL
import scala.reflect.ClassTag

abstract class AbstractPacketTraffic(override val currentIdentifier: String,
                                     defaultPersistenceConfigUrl: Option[URL]) extends PacketTraffic {

    private  val context                 : ImmutablePersistenceContext = ImmutablePersistenceContext()
    private  val minimalConfigBuilder                                  = PersistenceConfigBuilder.fromScript(getClass.getResource("/default_scripts/persistence_minimal.sc"), this)
    private  val omcNode                                               = createObjectManagementChannel()
    private  val objectChannel                                         = omcNode.injectable
    override val defaultPersistenceConfig: PersistenceConfig           = {
        defaultPersistenceConfigUrl
                .fold(new PersistenceConfigBuilder())(PersistenceConfigBuilder.fromScript(_, this))
                .transfer(minimalConfigBuilder)
                .build(context, null, objectChannel)
    }

    override  val reference  : TrafficReference      = TrafficReference
    override  val presence   : NetworkObjectPresence = SystemNetworkObjectPresence
    override  val trafficPath: Array[Int]            = Array.empty
    private   val linker                             = new TrafficNetworkObjectLinker(objectChannel, this)
    protected val rootStore                          = new SimplePacketInjectableStore(this, linker, defaultPersistenceConfig, trafficPath)
    @volatile private var closed                     = false

    override def getTrafficObjectLinker: NetworkObjectLinker[TrafficReference] with TrafficInterestedNPH = {
        linker
    }

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): TrafficNode[C] = {
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
        processInjection(SimplePacketBundle(packet, attr, coordinates))
    }

    override def processInjection(bundle: PacketBundle): Unit = {
        val path = bundle.coords.path
        findNode(path).get.injectable.inject(bundle)
    }

    override def processInjection(result: ObjectDeserializationResult): Unit = {
        if (result.isDeserialized) {
            val bundle = new PacketBundle {
                override val packet    : Packet            = result.packet
                override val attributes: PacketAttributes  = result.attributes
                override val coords    : PacketCoordinates = result.coords
            }
            result.informInjected
            processInjection(bundle)
        } else {
            val path = result.coords.path
            val node = findNode(path).getOrElse {
                throw new NoSuchTrafficPresenceException(s"Could not process injection: Could not find packet injectable located at ${path.mkString("/")}")
            }
            node.ipu().post(result, node.injectable)
        }
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

    protected def ensureOpen(): Unit = {
        if (closed)
            throw new ClosedException("This Traffic handler is closed")
    }

    private def createObjectManagementChannel(): TrafficNode[ObjectManagementChannel] = {
        val objectChannelConfig = {
            val linker = new ObjectChannelContextObjectLinker(minimalConfigBuilder)
            new SimplePersistenceConfig(
                context, new ClassMap(), linker,
                false, true
            )
        }
        val objectChannel       = {
            val scope = ChannelScopes.BroadcastScope(newWriter(Array.empty, objectChannelConfig), Array.empty)
            new DefaultObjectManagementChannel(null, scope)
        }
        new TrafficNode[ObjectManagementChannel]{
            override val injectable       : ObjectManagementChannel = objectChannel
            override val persistenceConfig: PersistenceConfig     = objectChannelConfig
            override val ipu: InjectionProcessorUnit = new SequentialInjectionProcessorUnit()
            override def setPerformantInjection(): this.type = throw new UnsupportedOperationException
            override def setSequentialInjection(): this.type = throw new UnsupportedOperationException
            override def preferPerformances(): Boolean = throw new UnsupportedOperationException
            override def chainIPU(path: Array[Int]): this.type = throw new UnsupportedOperationException
        }
    }

}