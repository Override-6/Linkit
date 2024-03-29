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

package fr.linkit.server.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.{ConnectionException, ExternalConnection}
import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.network.{ExternalConnectionState, Network}
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.obj.TrafficObjectReference
import fr.linkit.api.gnom.persistence.{ObjectTranslator, PacketTransfer, PacketUpload}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import org.jetbrains.annotations.NotNull

import java.net.Socket
import java.util.concurrent.Future

class ServerExternalConnection private(val session: ExternalConnectionSession) extends ExternalConnection {

    import session._

    override val currentName   : String           = server.currentName
    override val traffic       : PacketTraffic    = server.traffic
    override val translator    : ObjectTranslator = server.translator
    override val network       : Network          = session.network
    override val port          : Int              = server.port
    override val boundNT       : NameTag          = session.boundNT
    @volatile private var alive: Boolean          = false
    private  val currentNT     : NameTag          = NameTag(currentName)
    private  val tnol                             = network.gnol.trafficNOL


    override def shutdown(): Unit = {
        alive = false

        readThread.close()
        session.close()

        connectionManager.unregister(currentNT)
        AppLoggers.Connection.info(s"Connection closed for $currentName")
    }

    override def isAlive: Boolean = alive

    override def getState: ExternalConnectionState = session.getSocketState


    override def runLater[A](f: => A): Future[A] = server.runLater(f)


    def start(): Unit = {
        if (alive) {
            throw ConnectionException(this, "This Connection was already used and is now definitely closed.")
        }
        alive = true
        readThread.onPacketRead = traffic.processInjection
        readThread.onReadException = () => runLater(shutdown())
        readThread.start()
        //Method useless but kept because services could need to be started in the future?
    }

    def sendPacket(packet: Packet, attributes: PacketAttributes, path: Array[Int]): Unit = {
        val coords       = DedicatedPacketCoordinates(path, boundNT, currentNT)
        val config       = traffic.getPersistenceConfig(coords.path)
        val transferInfo = SimpleTransferInfo(coords, attributes, packet, config, network)
        val result       = translator.translate(transferInfo)
        send(result)
    }

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    private[connection] def updateSocket(socket: Socket): Unit = {
        session.updateSocket(socket)
    }

    def canHandlePacketInjection(result: PacketTransfer): Boolean = {
        val channelPath = result.coords.path
        channelPath.length == 0 || {
            val reference = new TrafficObjectReference(channelPath)
            val present   = tnol.isPresentOnEngine(boundNT, reference)
            present
        }
    }

    def send(result: PacketUpload): Unit = {
        if (!canHandlePacketInjection(result)) {
            val channelPath = result.coords.path
            val reference   = new TrafficObjectReference(channelPath)
            throw new PacketNotInjectableException(this, s"Engine '$boundNT' does not contains any traffic packet injectable presence at $reference.")
        }
        session.send(result)
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
