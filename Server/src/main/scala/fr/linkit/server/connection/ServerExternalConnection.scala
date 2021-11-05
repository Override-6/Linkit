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

package fr.linkit.server.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.{ConnectionException, ExternalConnection}
import fr.linkit.api.gnom.network.{ExternalConnectionState, Network}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.gnom.persistence.obj.TrafficPresenceReference
import fr.linkit.api.gnom.persistence.{ObjectTranslator, PacketTransferResult}
import fr.linkit.api.internal.concurrency.{AsyncTask, WorkerPools, workerExecution}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.api.internal.system.event.EventNotifier
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import fr.linkit.engine.internal.system.SystemPacket
import org.jetbrains.annotations.NotNull

import java.net.Socket
import java.nio.ByteBuffer

class ServerExternalConnection private(val session: ExternalConnectionSession) extends ExternalConnection {

    import session._

    override val currentIdentifier: String           = server.currentIdentifier
    override val traffic          : PacketTraffic    = server.traffic
    override val translator       : ObjectTranslator = server.translator
    override val eventNotifier    : EventNotifier    = server.eventNotifier
    override val network          : Network          = session.network
    override val port             : Int              = server.port
    override val boundIdentifier  : String           = session.boundIdentifier
    @volatile private var alive                      = false
    private  val tnol                                = network.gnol.trafficNOL

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
        /*runLater*/
        {
            val coords       = DedicatedPacketCoordinates(path, boundIdentifier, server.currentIdentifier)
            val config       = traffic.getPersistenceConfig(coords.path)
            val transferInfo = SimpleTransferInfo(coords, attributes, packet, config, network.gnol)
            val result       = translator.translate(transferInfo)
            send(result)
        }
    }

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    private[connection] def updateSocket(socket: Socket): Unit = {
        WorkerPools.ensureCurrentIsWorker()
        session.updateSocket(socket)
    }

    def canHandlePacketInjection(result: PacketTransferResult): Boolean = {
        val channelPath = result.coords.path
        channelPath.length == 0 || {
            val reference = new TrafficPresenceReference(channelPath)
            tnol.isPresentOnEngine(boundIdentifier, reference)
        }
    }

    def send(result: PacketTransferResult): Unit = {
        if (!canHandlePacketInjection(result)) {
            val channelPath = result.coords.path
            val reference   = new TrafficPresenceReference(channelPath)
            throw new PacketNotInjectableException(this, s"Engine '$boundIdentifier' does not contains any traffic packet injectable presence at $reference.")
        }
        session.send(result.buff)
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
