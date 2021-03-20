package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.{ConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier

import scala.reflect.ClassTag

class ServerConnection extends ConnectionContext {
    override val configuration: ConnectionConfiguration = _
    override val boundIdentifier: String = _

    override def traffic: PacketTraffic = ???

    override def network: Network = ???

    override def translator: PacketTranslator = ???

    override val eventNotifier: EventNotifier = _

    override def getState: ConnectionState = ???

    override def shutdown(): Unit = ???

    override def isAlive(): Boolean = ???

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = ???

    override def runLater(task: => Unit): Unit = ???

    def getConnection(identifier: String): ConnectionContext = ???
}
