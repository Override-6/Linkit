/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.application.network.{ExternalConnectionState, Network}
import fr.linkit.api.application.packet.channel.ChannelScope
import fr.linkit.api.application.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.{PacketSerializationResult, PacketTranslator}
import fr.linkit.api.application.packet.traffic.{PacketInjectable, PacketInjectableFactory, PacketInjectableStore, PacketTraffic}
import fr.linkit.api.application.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.application.{ApplicationContext, ExternalConnection}
import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.api.internal.concurrency.{AsyncTask, WorkerPools, workerExecution}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.api.internal.system.event.EventNotifier
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import fr.linkit.engine.internal.system.SystemPacket
import org.jetbrains.annotations.NotNull

import java.net.Socket
import java.nio.ByteBuffer
import scala.reflect.ClassTag

class ServerExternalConnection private(val session: ExternalConnectionSession) extends ExternalConnection {

    import session._

    override val currentIdentifier       : String            = server.currentIdentifier
    override val traffic                 : PacketTraffic     = server.traffic
    override val translator              : PacketTranslator  = server.translator
    override val eventNotifier           : EventNotifier     = server.eventNotifier
    override val network                 : Network           = session.network
    override val port                    : Int               = server.port
    override val boundIdentifier         : String            = session.boundIdentifier
    override val defaultPersistenceConfig: PersistenceConfig = server.defaultPersistenceConfig
    override val trafficPath             : Array[Int]        = server.trafficPath
    @volatile private var alive                              = false

    override def shutdown(): Unit = {
        WorkerPools.ensureCurrentIsWorker()
        alive = false
        /*if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }*/
        readThread.close()
        session.close()

        connectionManager.unregister(currentIdentifier)
        AppLogger.trace(s"Connection closed for $currentIdentifier")
    }

    override def isAlive: Boolean = alive

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C = {
        traffic.getInjectable(injectableID, config, factory, scopeFactory)
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = traffic.findStore(id)

    override def createStore(id: Int, config: PersistenceConfig): PacketInjectableStore = traffic.createStore(id, config)

    override def getState: ExternalConnectionState = session.getSocketState

    override def runLaterControl[A](@workerExecution task: => A): AsyncTask[A] = {
        server.runLaterControl(task)
    }

    override def runLater(task: => Unit): Unit = server.runLater(task)

    def start(): Unit = {
        if (alive) {
            throw ConnectionException(this, "This Connection was already used and is now definitely closed.")
        }
        alive = true
        readThread.onPacketRead = result => {
            val coordinates: DedicatedPacketCoordinates = result.coords match {
                case d: DedicatedPacketCoordinates => d
                case _                             => throw new IllegalArgumentException("Packet must be dedicated to this connection.")
            }

            handlePacket(result.packet, result.attributes, coordinates)
        }
        readThread.start()
        //Method useless but kept because services could need to be started in the future?
    }

    def sendPacket(packet: Packet, attributes: PacketAttributes, path: Array[Int]): Unit = {
        runLater {
            val coords       = DedicatedPacketCoordinates(path, boundIdentifier, server.currentIdentifier)
            val config       = traffic.getPersistenceConfig(coords.path)
            val transferInfo = SimpleTransferInfo(coords, attributes, packet, config)
            val result       = translator.translate(transferInfo)
            session.send(result)
        }
    }

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    private[connection] def updateSocket(socket: Socket): Unit = {
        WorkerPools.ensureCurrentIsWorker()
        session.updateSocket(socket)
    }

    def send(result: PacketSerializationResult): Unit = {
        session.send(result)
    }

    private[connection] def send(buff: ByteBuffer): Unit = {
        session.send(buff)
    }

    @workerExecution
    private def handlePacket(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit = {
        if (!alive)
            return

        AppLogger.vWarn(s"HANDLING PACKET $packet, $attributes, $coordinates")

        packet match {
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case _: Packet                  =>
                serverTraffic.processInjection(packet, attributes, coordinates)
        }
    }

    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        import fr.linkit.engine.internal.system.SystemOrder._
        orderType match {
            case CLIENT_CLOSE => runLater(shutdown())
            case SERVER_CLOSE => server.shutdown()

            case _ =>
                val msg = s"Could not complete order '$orderType', can't be handled by a server or unknown order"
                AppLogger.error(msg)
            //UnexpectedPacketException(s"Could not complete order '$orderType', can't be handled by a server or unknown order")
            //.printStackTrace(getConsoleErr)
        }
    }

    override def getApp: ApplicationContext = server.getApp
}

object ServerExternalConnection {

    /**
     * Constructs a ClientConnection without starting it.
     *
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def open(@NotNull session: ExternalConnectionSession): ServerExternalConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ServerExternalConnection(session)
        connection.start()
        connection
    }

}
